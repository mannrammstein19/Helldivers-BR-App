package br.com.helldiversbr.app.ui.screens

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.PathParser
import androidx.core.graphics.drawable.toBitmap
import br.com.helldiversbr.app.data.Planet
import br.com.helldiversbr.app.data.SiteAssets
import br.com.helldiversbr.app.ui.theme.HD
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.*

private data class MapArt(val bitmaps: Map<String, Bitmap> = emptyMap(), val failed: Int = 0, val done: Boolean = false)

@Composable
fun GalaxyCanvas(planets: List<Planet>, all: List<Planet>, routes: Boolean, sectors: Boolean, territories: Boolean,
                 invasions: Boolean, selected: Long?, active: Set<Long>, dssHost: Long?, onSelect: (Long) -> Unit) {
    val context = LocalContext.current
    var zoom by remember { mutableStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    val art by produceState(MapArt(), context) {
        val keys = listOf("human", "automaton", "terminid", "illuminate", "earth", "defense", "liberation",
            "galaxy", "penta", "meridia", "wreckage", "hive_lord", "draco_barata")
        val loaded = coroutineScope {
            keys.map { key -> async {
                var bitmap: Bitmap? = null
                for (url in SiteAssets.urls(context, key)) {
                    val result = context.imageLoader.execute(ImageRequest.Builder(context).data(url)
                        .size(if (key == "galaxy") 1200 else 192).allowHardware(false).build())
                    if (result is SuccessResult) {
                        bitmap = result.drawable.toBitmap(); break
                    }
                }
                key to bitmap
            } }.awaitAll()
        }
        value = MapArt(loaded.mapNotNull { (key, bitmap) -> bitmap?.let { key to it } }.toMap(), loaded.count { it.second == null }, true)
    }
    val outlines by produceState<Map<String, Path>>(emptyMap()) {
        value = withContext(Dispatchers.IO) {
            context.assets.open("sectors.json").bufferedReader().use {
                Json.decodeFromString(MapSerializer(String.serializer(), String.serializer()), it.readText())
                    .mapValues { (_, path) -> requireNotNull(PathParser.createPathFromPathData(path)) }
            }
        }
    }
    val positioned = remember(planets, all) { planets.mapNotNull { p -> mapPosition(p, all)?.let { p to Offset(it.x.toFloat() * 500f, -it.y.toFloat() * 500f) } } }
    val positions = remember(positioned) { positioned.associate { it.first.index to it.second } }
    val enemySectors = remember(all) {
        val result = mutableMapOf<String, MutableSet<String>>()
        all.filter { mapPosition(it, all) != null }.forEach { p ->
            val set = result.getOrPut(sectorKey(p.sector)) { mutableSetOf() }
            listOfNotNull(mapFaction(p.currentOwner), p.event?.let { mapFaction(it.faction) })
                .filter { it != "human" && it != "unknown" }.forEach { set += it }
        }
        result.mapValues { it.value.sorted() }
    }
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { zoom = (zoom / 1.4f).coerceAtLeast(1f) }) { Text("−") }
            TextButton(onClick = { zoom = 1f; pan = Offset.Zero }) { Text("CENTRALIZAR") }
            TextButton(onClick = { zoom = (zoom * 1.4f).coerceAtMost(10f) }) { Text("+") }
        }
        Canvas(Modifier.fillMaxWidth().height(430.dp).clip(RoundedCornerShape(14.dp))
            .border(1.dp, HD.Border, RoundedCornerShape(14.dp)).background(Color(0xFF050810))
            .pointerInput(Unit) {
                detectTransformGestures { centroid, move, scale, _ ->
                    val next = (zoom * scale).coerceIn(1f, 10f)
                    val center = Offset(size.width / 2f, size.height / 2f)
                    pan = centroid - center - (centroid - center - pan) * (next / zoom) + move
                    val limit = max(size.width, size.height) * next
                    pan = Offset(pan.x.coerceIn(-limit, limit), pan.y.coerceIn(-limit, limit)); zoom = next
                }
            }
            .pointerInput(positioned) {
                detectTapGestures { tap ->
                    val unit = min(size.width, size.height) / 1120f * zoom
                    val origin = Offset(size.width / 2f, size.height / 2f) + pan
                    positioned.minByOrNull { (_, point) -> (origin + point * unit - tap).getDistance() }?.let { (planet, point) ->
                        if ((origin + point * unit - tap).getDistance() <= 24.dp.toPx()) onSelect(planet.index)
                    }
                }
            }) {
            val canvas = drawContext.canvas.nativeCanvas
            val unit = size.minDimension / 1120f * zoom
            val origin = center + pan
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textAlign = Paint.Align.CENTER; color = android.graphics.Color.WHITE
                textSize = 9.sp.toPx() / unit; setShadowLayer(2f / unit, 0f, 1f / unit, android.graphics.Color.BLACK)
            }
            val now = System.currentTimeMillis()
            canvas.save(); canvas.translate(origin.x, origin.y); canvas.scale(unit, unit)
            fun circle(at: Offset, radius: Float, color: Color, stroke: Float = 0f) {
                paint.shader = null; paint.color = color.toArgb(); paint.style = if (stroke > 0) Paint.Style.STROKE else Paint.Style.FILL; paint.strokeWidth = stroke
                canvas.drawCircle(at.x, at.y, radius, paint)
            }
            fun icon(key: String, at: Offset, width: Float, height: Float = width): Boolean {
                val image = art.bitmaps[key] ?: return false
                paint.shader = null; paint.style = Paint.Style.FILL; paint.color = android.graphics.Color.WHITE
                // Fit rather than stretching source artwork.
                val ratio = min(width / image.width, height / image.height)
                val w = image.width * ratio; val h = image.height * ratio
                canvas.drawBitmap(image, null, RectF(at.x - w / 2, at.y - h / 2, at.x + w / 2, at.y + h / 2), paint)
                return true
            }
            fun arc(at: Offset, radius: Float, percent: Double?, color: Color, stroke: Float) {
                circle(at, radius, Color(0xFF303845), stroke)
                if (percent != null && percent > 0) {
                    paint.color = color.toArgb(); paint.style = Paint.Style.STROKE; paint.strokeWidth = stroke
                    canvas.drawArc(RectF(at.x-radius, at.y-radius, at.x+radius, at.y+radius), -90f, (percent.coerceIn(0.0,100.0)*3.6).toFloat(), false, paint)
                }
            }
            fun label(text: String, at: Offset, color: Color = Color.White, small: Boolean = false) {
                textPaint.color = color.toArgb(); textPaint.textSize = (if (small) 8.sp else 10.sp).toPx() / unit
                canvas.drawText(text, at.x, at.y, textPaint)
            }
            // Exact 1000x1000 backdrop/55 SVG contours from mapa-classico.js.
            canvas.save()
            canvas.clipPath(Path().apply { addCircle(0f, 0f, 500f, Path.Direction.CW) })
            icon("galaxy", Offset.Zero, 1000f)
            paint.style = Paint.Style.FILL; paint.color = Color.Black.copy(alpha=.35f).toArgb()
            canvas.drawCircle(0f, 0f, 500f, paint)
            outlines.forEach { (key, path) ->
                val enemies = enemySectors[key].orEmpty()
                if (territories && enemies.isNotEmpty()) {
                    paint.style = Paint.Style.FILL; paint.color = mapColor(enemies.first()).copy(alpha=.20f).toArgb()
                    if (enemies.size > 1) {
                        val bounds = RectF(); path.computeBounds(bounds, true)
                        paint.shader = android.graphics.LinearGradient(bounds.left,bounds.top,bounds.right,bounds.bottom,
                            enemies.map { mapColor(it).copy(alpha=.20f).toArgb() }.toIntArray(), null, android.graphics.Shader.TileMode.CLAMP)
                    }
                    canvas.drawPath(path,paint); paint.shader=null
                }
                if (sectors) {
                    paint.style=Paint.Style.STROKE; paint.strokeWidth=.8f/unit
                    paint.color=(enemies.firstOrNull()?.let { mapColor(it).copy(alpha=.48f) } ?: Color(0xFF59606C).copy(alpha=.4f)).toArgb()
                    canvas.drawPath(path,paint)
                }
            }
            canvas.restore()
            listOf(Triple("automaton",215f,45f),Triple("terminid",280f,45f),Triple("illuminate",113f,-46f)).forEach { (key,start,sweep) ->
                val path=Path().apply { addArc(RectF(-535f,-535f,535f,535f),start,sweep) }
                val title=when(key){"automaton"->"AUTÔMATOS";"terminid"->"TERMINÍDEOS";else->"ILUMINADOS"}
                textPaint.color=mapColor(key).toArgb();textPaint.textSize=12f;textPaint.textAlign=Paint.Align.LEFT
                val length=android.graphics.PathMeasure(path,false).length
                canvas.drawTextOnPath(title,path,(length-textPaint.measureText(title))/2f,0f,textPaint)
                textPaint.textAlign=Paint.Align.CENTER
            }
            val indexed = positioned.associate { it.first.index to it.first }
            if (routes) {
                val drawn = mutableSetOf<Pair<Long,Long>>()
                positioned.forEach { (p,a) -> p.waypoints.forEach { id ->
                    val edge = min(p.index,id) to max(p.index,id)
                    val b = positions[id]
                    if (b != null && drawn.add(edge)) {
                        val front=mapFaction(p.currentOwner)!=mapFaction(indexed.getValue(id).currentOwner)
                        paint.style=Paint.Style.STROKE; paint.strokeWidth=(if(front) 1f else .65f)/unit
                        paint.color=(if(front) Color(0xFF91B7C7) else Color(0xFF526577)).copy(alpha=.6f).toArgb()
                        canvas.drawLine(a.x,a.y,b.x,b.y,paint)
                    }
                } }
            }
            if (invasions) positioned.forEach { (source,a) ->
                val attacker=mapFaction(source.currentOwner)
                if (mapSpecial(source)==null && attacker!="unknown") source.attacking.forEach { id ->
                    val target=indexed[id]; val b=positions[id]
                    val allowed=target!=null && mapSpecial(target)==null && if(attacker=="human") source.event==null && mapOffensive(target,active)
                        else target.event!=null && mapFaction(target.currentOwner)=="human" && mapFaction(target.event.faction) in listOf("unknown",attacker)
                    if(allowed && b!=null && id!=source.index) {
                        val dir=b-a; val length=dir.getDistance(); if(length>0f) {
                            val d=dir/length; val end=b-d*15f; val normal=Offset(-d.y,d.x)
                            paint.style=Paint.Style.STROKE;paint.strokeWidth=1.6f/unit;paint.color=mapColor(attacker).toArgb()
                            canvas.drawLine(a.x,a.y,end.x,end.y,paint)
                            val tip=Path().apply{moveTo(end.x,end.y);lineTo(end.x-d.x*9+normal.x*4,end.y-d.y*9+normal.y*4);lineTo(end.x-d.x*9-normal.x*4,end.y-d.y*9-normal.y*4);close()}
                            paint.style=Paint.Style.FILL;canvas.drawPath(tip,paint)
                        }
                    }
                }
            }
            positioned.sortedBy { mapEarth(it.first) }.forEach { (p,at) ->
                val screen = origin + at * unit
                if(screen.x < -100 || screen.y < -100 || screen.x > size.width+100 || screen.y > size.height+100) return@forEach
                val special=mapSpecial(p);val capital=mapEarth(p);val key=mapFaction(p.currentOwner)
                val defense=special==null&&p.event!=null;val offensive=mapOffensive(p,active)
                val r=max(1000f/260f,1.8.dp.toPx()/unit)*(if(capital)1.9f else 1f)
                val color=mapColor(key);val detail=zoom>=2.3f || selected==p.index || positioned.size<=12
                if(mapName(p)=="cyberstan") {
                    paint.style=Paint.Style.FILL;paint.shader=android.graphics.RadialGradient(at.x,at.y,r*6f,intArrayOf(0x339BD589,0x009BD589),null,android.graphics.Shader.TileMode.CLAMP)
                    canvas.drawOval(at.x-r*6,at.y-r*3,at.x+r*6,at.y+r*3,paint);paint.shader=null
                }
                if(capital) listOf(4f,3f,2.2f).forEachIndexed { i,v->circle(at,r*v,Color(0xFFFFE68A).copy(alpha=.025f+i*.015f)) }
                circle(at,r*(if(defense||offensive)2.7f else 2.1f),color.copy(alpha=.12f))
                if(defense||offensive) arc(at,r*2.08f,mapProgress(p),if(defense)Color(0xFF4DA6FF)else mapColor("human"),r*.48f)
                if(defense) arc(at,r*2.72f,mapInvasionProgress(p,now),mapColor(mapFaction(p.event!!.faction)),r*.32f)
                val dotR=if(defense||offensive)r*1.38f else r
                paint.style=Paint.Style.FILL
                paint.shader=android.graphics.RadialGradient(at.x-dotR*.3f,at.y-dotR*.35f,dotR*1.7f,
                    intArrayOf(color.copy(alpha=1f).toArgb(),(if(key=="human"&&!capital)Color(0xFF5089A2)else color).toArgb(),Color(0xFF111820).toArgb()),null,android.graphics.Shader.TileMode.CLAMP)
                canvas.drawCircle(at.x,at.y,dotR,paint);paint.shader=null
                val iconSize=r*(if(key=="human") {if(defense||offensive)2.76f else 2f} else {if(defense||offensive)2.05f else 1.55f})
                if(capital) { if(!icon("earth",at,r*2)) icon("human",at,iconSize) }
                else if(special!=null) {
                    if(!icon(special,at,r*5)) {circle(at,r*1.7f,Color.Black);circle(at,r*1.8f,color,.7f/unit)}
                } else icon(if(key=="unknown")"human" else key,at,iconSize)
                if(defense||offensive) icon(if (defense) "defense" else "liberation",at+Offset(-r*3.1f,-r*3.9f),r*2.2f)
                if(defense) {
                    val attacker=mapFaction(p.event!!.faction); val badge=at+Offset(r*2.65f,-r*2.65f)
                    circle(badge,r*1.05f,Color(0xFF090D12));circle(badge,r*1.05f,mapColor(attacker),.6f/unit);icon(attacker,badge,r*1.8f)
                }
                if(mapName(p)=="omicron") {icon("hive_lord",at+Offset(r*4.65f,-r*3.25f),r*2.5f);icon("draco_barata",at+Offset(r*5.35f,-r*.25f),r*2.5f)}
                if(dssHost==p.index) {
                    val dssAt=at+Offset(0f,-r*3.2f);val d=r*.95f
                    val diamond=Path().apply {moveTo(dssAt.x,dssAt.y-d);lineTo(dssAt.x+d,dssAt.y);lineTo(dssAt.x,dssAt.y+d);lineTo(dssAt.x-d,dssAt.y);close()}
                    paint.style=Paint.Style.FILL;paint.color=Color(0xFFFFD23F).toArgb();canvas.drawPath(diamond,paint)
                    if(detail) label("DSS",dssAt+Offset(0f,-d*1.9f),Color(0xFFFFD23F),true)
                }
                if(selected==p.index) circle(at,r*3.4f,Color.White,1.3f/unit)
                if(detail) {
                    val gap=12.sp.toPx()/unit
                    label(if (capital) "Super Terra" else p.nameText,at+Offset(0f,r*3.2f+gap),Color.White)
                    label("${mapPlayerCount(p.statistics.playerCount)} HD",at+Offset(0f,r*3.2f+gap*2),Color(0xFFADB7C5),true)
                    if(defense||offensive) {
                        label(mapPercent(mapProgress(p)),at+Offset(0f,-r*3.5f),if(defense)Color(0xFF4DA6FF)else mapColor("human"))
                        if(defense) label("INVASÃO ${mapPercent(mapInvasionProgress(p,now))}",at+Offset(0f,-r*3.5f-gap),mapColor(mapFaction(p.event!!.faction)),true)
                    }
                }
            }
            canvas.restore()
        }
        if(art.done&&art.failed>0) Text("${art.failed} imagens do mapa não carregaram. Verifique a conexão e os arquivos do site.",color=HD.Gold,fontSize=11.sp)
        if(positioned.isEmpty()) Text("Sem coordenadas válidas para este filtro.",color=HD.TextDim,fontSize=12.sp)
    }
}
