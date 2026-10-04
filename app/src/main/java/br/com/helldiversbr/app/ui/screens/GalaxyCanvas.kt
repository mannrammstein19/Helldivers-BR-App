package br.com.helldiversbr.app.ui.screens

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
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
import br.com.helldiversbr.app.data.MapAssets
import br.com.helldiversbr.app.data.PlanetPresences
import br.com.helldiversbr.app.data.planetTitle
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
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

private data class MapRoute(val a: Offset, val b: Offset, val from: Planet, val to: Planet)

private data class MapCaption(val text: String, val at: Offset, val color: Color, val small: Boolean = false)

private data class MapArt(val bitmaps: Map<String, Bitmap> = emptyMap(), val failed: Int = 0, val done: Boolean = false)

@Composable
fun GalaxyCanvas(planets: List<Planet>, all: List<Planet>, routes: Boolean, sectors: Boolean, territories: Boolean,
                 invasions: Boolean, selected: Long?, active: Set<Long>, dssHost: Long?, onSelect: (Long) -> Unit, modifier: Modifier = Modifier, options: MapDisplayOptions = MapDisplayOptions(), stale: Boolean = false, readAtMillis: Long = 0L, dssLive: Boolean = false, routeFocus: Set<Long>? = null) {
    val context = LocalContext.current
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    var zoom by remember { mutableStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    val art by produceState(MapArt(), context) {
        val files = MapAssets.all().filterKeys { it in listOf("human", "automaton", "terminid", "illuminate", "earth", "defense", "liberation", "galaxy", "penta", "meridia", "wreckage", "hive-lord", "draco-barata", "dss-operacional", "dss-inoperante") || it.startsWith("nave-") || it.startsWith("imagens/guerra/presencas/") } +
            MapAssets.planetEntries().mapKeys { "planet:${it.key}" }
        val semaphore = Semaphore(8)
        val loaded = coroutineScope {
            files.map { (key, file) -> async { semaphore.withPermit {
                val result = context.imageLoader.execute(ImageRequest.Builder(context).data("file:///android_asset/$file")
                    .size(if (key == "galaxy") 1200 else if(key.startsWith("planet:")) 96 else 192).allowHardware(false).build())
                key to if(result is SuccessResult) result.drawable.toBitmap() else null
            } } }.awaitAll()
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
    val attacked = remember(positioned) { positioned.filter { isPlanetUnderAttack(it.first) } }
    // Apenas esta camada leve é redesenhada; geografia, imagens e dados continuam estáticos.
    val alertPhase = produceState(0f, attacked.isNotEmpty() && options.motion && !stale, lifecycle) {
        if (attacked.isNotEmpty() && options.motion && !stale) lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
            val start = android.os.SystemClock.elapsedRealtime()
            while (true) {
                value = ((android.os.SystemClock.elapsedRealtime() - start) % 1_800) / 1_800f
                kotlinx.coroutines.delay(50)
            }
        }
    }
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
    // Static geography is rasterized only when its inputs change, never on pinch frames.
    val backdrop = remember(art, outlines, enemySectors, territories, sectors, options.stripes) {
        Bitmap.createBitmap(1200, 1200, Bitmap.Config.ARGB_8888).also { bitmap ->
            val c = android.graphics.Canvas(bitmap)
            c.translate(600f, 600f); c.scale(1.2f, 1.2f)
            c.clipPath(Path().apply { addCircle(0f, 0f, 500f, Path.Direction.CW) })
            val brush = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
            art.bitmaps["galaxy"]?.let { c.drawBitmap(it, null, RectF(-500f,-500f,500f,500f), brush) }

            outlines.forEach { (key, path) ->
                val enemies = enemySectors[key].orEmpty()
                if (territories && enemies.isNotEmpty()) {
                    brush.style = Paint.Style.FILL
                    brush.color = mapColor(enemies.first()).copy(alpha = .20f).toArgb()
                    if (enemies.size > 1) {
                        val bounds = RectF(); path.computeBounds(bounds, true)
                        brush.shader = android.graphics.LinearGradient(bounds.left,bounds.top,bounds.right,bounds.bottom,
                            enemies.map { mapColor(it).copy(alpha = .20f).toArgb() }.toIntArray(), null, android.graphics.Shader.TileMode.CLAMP)
                    }
                    c.drawPath(path, brush); brush.shader = null
                    if (options.stripes) {
                        c.save(); c.clipPath(path)
                        brush.style = Paint.Style.STROKE; brush.strokeWidth = 2f
                        brush.color = mapColor(enemies.first()).copy(alpha = .10f).toArgb()
                        for (i in -1000..1000 step 18) c.drawLine(i.toFloat(),-500f,i+700f,500f,brush)
                        c.restore()
                    }
                }
                if (sectors) {
                    brush.style = Paint.Style.STROKE; brush.strokeWidth = 1.5f
                    brush.color = (enemies.firstOrNull()?.let { mapColor(it).copy(alpha = .48f) } ?: Color(0x6659606C)).toArgb()
                    c.drawPath(path, brush)
                }
            }
        }
    }
    val indexed = remember(positioned) { positioned.associate { it.first.index to it.first } }
    val edges = remember(positioned, routeFocus) {
        val seen = mutableSetOf<Pair<Long, Long>>()
        positioned.flatMap { (p, a) -> p.waypoints.mapNotNull { id ->
            val b = positions[id]
            if (b != null && (b-a).getDistance()>0 && (routeFocus == null || p.index in routeFocus || id in routeFocus) && seen.add(min(p.index,id) to max(p.index,id)))
                MapRoute(a, b, p, indexed.getValue(id))
            else null
        } }
    }
    val drawOrder = remember(positioned, active, selected) {
        positioned.sortedBy { (p, _) -> when {
            p.index == selected -> 0
            p.event != null || p.index in active -> 1
            mapFaction(p.currentOwner) != "human" -> 2
            else -> 3
        } }
    }
    val captions = remember(positioned) { positioned.associate { (p, _) ->
        p.index to (planetTitle(p.nameText) to "${mapPlayerCount(p.statistics.playerCount)} HD")
    } }
    // Selecting a search result or marker brings it below the floating summary.
    // Live API updates do not reset the camera.
    LaunchedEffect(selected, viewport) {
        val point = selected?.let { positions[it] }
        if (point != null && viewport.width > 0 && viewport.height > 0) {
            zoom = max(zoom, 2.5f)
            val unit = min(viewport.width, viewport.height) / 1120f * zoom
            pan = if (viewport.width > viewport.height)
                Offset(-point.x * unit + viewport.width * .23f, -point.y * unit)
            else Offset(-point.x * unit, -point.y * unit + viewport.height * .18f)
        }
    }
    val paint = remember { Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true } }
    val textPaint = remember { Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER } }
    Box(modifier.clipToBounds()) {
        Canvas(Modifier.fillMaxSize().onSizeChanged { viewport = it }.background(Color(0xFF050810))
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
            textPaint.setShadowLayer(2f / unit, 0f, 1f / unit, android.graphics.Color.BLACK)
            val occupied = mutableListOf<RectF>()
            val pendingLabels = mutableListOf<MapCaption>()
            val now = if(stale) readAtMillis else System.currentTimeMillis()
            canvas.save(); canvas.translate(origin.x, origin.y); canvas.scale(unit, unit)
            fun circle(at: Offset, radius: Float, color: Color, stroke: Float = 0f) {
                paint.shader = null; paint.color = color.toArgb(); paint.style = if (stroke > 0) Paint.Style.STROKE else Paint.Style.FILL; paint.strokeWidth = stroke
                canvas.drawCircle(at.x, at.y, radius, paint)
            }
            fun icon(key: String, at: Offset, width: Float, height: Float = width, alpha: Int = 255, tint: Color? = null): Boolean {
                val image = art.bitmaps[key] ?: return false
                paint.shader = null; paint.style = Paint.Style.FILL; paint.color = android.graphics.Color.WHITE
                paint.alpha = alpha
                paint.colorFilter = tint?.let { android.graphics.PorterDuffColorFilter(it.toArgb(), android.graphics.PorterDuff.Mode.SRC_IN) }
                // Fit rather than stretching source artwork.
                val ratio = min(width / image.width, height / image.height)
                val w = image.width * ratio; val h = image.height * ratio
                canvas.drawBitmap(image, null, RectF(at.x - w / 2, at.y - h / 2, at.x + w / 2, at.y + h / 2), paint)
                paint.alpha = 255; paint.colorFilter = null
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
                textPaint.color = color.toArgb(); textPaint.textSize = (if (small) 8.sp else 11.sp).toPx() / unit
                canvas.drawText(text, at.x, at.y, textPaint)
            }
            paint.shader = null; paint.style = Paint.Style.FILL; paint.color = android.graphics.Color.WHITE
            canvas.drawBitmap(backdrop, null, RectF(-500f,-500f,500f,500f), paint)
            if (routes) edges.forEach { (a, b, source, target) ->
                val sa = origin + a * unit; val sb = origin + b * unit
                if (max(sa.x,sb.x) >= 0 && min(sa.x,sb.x) <= size.width &&
                    max(sa.y,sb.y) >= 0 && min(sa.y,sb.y) <= size.height) {
                    paint.style = Paint.Style.STROKE; paint.strokeWidth = 2.2f / unit
                    val ca = mapColor(mapFaction(source.currentOwner)).toArgb()
                    val cb = mapColor(mapFaction(target.currentOwner)).toArgb()
                    paint.shader = android.graphics.LinearGradient(a.x,a.y,b.x,b.y,intArrayOf(ca,ca,cb,cb),floatArrayOf(0f,.4f,.6f,1f),android.graphics.Shader.TileMode.CLAMP)
                    canvas.drawLine(a.x,a.y,b.x,b.y,paint); paint.shader = null
                }
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
            drawOrder.forEach { (p,at) ->
                val screen = origin + at * unit
                if(screen.x < -100 || screen.y < -100 || screen.x > size.width+100 || screen.y > size.height+100) return@forEach
                val special=mapSpecial(p);val capital=mapEarth(p);val key=mapFaction(p.currentOwner)
                val defense=special==null&&p.event!=null;val offensive=mapOffensive(p,active) && (mapProgress(p) ?: 0.0) >= .5
                val r = max(1000f / 260f, (if (defense || offensive) 3.4.dp else 1.8.dp).toPx() / unit) * (if (capital) 1.9f else 1f)
                val quiet = key == "human" && !defense && !offensive && selected != p.index
                val color = if (quiet) Color(0xFF617984) else mapColor(key)
                val detail = selected == p.index || zoom >= (if (quiet) 5f else if (defense || offensive) 1.6f else 2.3f)
                if(mapName(p)=="cyberstan") {
                    paint.style=Paint.Style.FILL;paint.shader=android.graphics.RadialGradient(at.x,at.y,r*6f,intArrayOf(0x339BD589,0x009BD589),null,android.graphics.Shader.TileMode.CLAMP)
                    canvas.drawOval(at.x-r*6,at.y-r*3,at.x+r*6,at.y+r*3,paint);paint.shader=null
                }
                if(capital) listOf(4f,3f,2.2f).forEachIndexed { i,v->circle(at,r*v,Color(0xFFFFE68A).copy(alpha=.025f+i*.015f)) }
                circle(at,r*(if(defense||offensive)2.7f else 2.1f),color.copy(alpha=.12f))
                if(defense||offensive) arc(at,r*2.08f,mapProgress(p),mapColor("human"),r*.48f)
                if(offensive) arc(at,r*2.72f,100.0-(mapProgress(p) ?: 0.0),color,r*.32f)
                if(defense) arc(at,r*2.72f,mapInvasionProgress(p,now),mapColor(mapFaction(p.event!!.faction)),r*.32f)
                val dotR=if(defense||offensive)r*1.38f else r
                paint.style=Paint.Style.FILL
                paint.shader = null; paint.color = color.toArgb()
                canvas.drawCircle(at.x, at.y, dotR, paint)
                val iconSize=r*(if(key=="human")2f else 1.55f)
                if(capital) { if(!icon("earth",at,r*2)) icon("human",at,iconSize) }
                else if(special!=null) {
                    if(!icon(special,at,r*5)) {circle(at,r*1.7f,Color.Black);circle(at,r*1.8f,color,.7f/unit)}
                } else if(!icon("planet:${p.index}",at,dotR*2)) icon(if(key=="unknown")"human" else key,at,iconSize)
                circle(at,dotR,color,.7f/unit)
                if(!defense && key !in listOf("human","unknown") && !options.clean) icon(key,at+Offset(0f,-r*2.3f),r*1.6f)
                if(options.progress && (defense||offensive)) {
                    val humanBadge = at+Offset(-r*3.8f,r*.9f)
                    icon(if(defense) "defense" else "liberation",humanBadge,r*1.8f)
                    if(defense) icon(mapFaction(p.event!!.faction),at+Offset(-r*3.8f,-r*1.8f),r*1.8f)
                }
                if(options.presences) {
                    val models = mutableSetOf<String>()
                    PlanetPresences.list(p).take(3).forEachIndexed { i, presence ->
                        val badge=at+Offset(r*3f, r*(i*3f-.4f))
                        paint.style=Paint.Style.STROKE; paint.color=mapColor(presence.faction).copy(alpha=.5f).toArgb(); paint.strokeWidth=.4f/unit
                        canvas.drawLine(at.x+dotR,at.y,badge.x-r,badge.y,paint)
                        icon("imagens/guerra/presencas/${presence.file}",badge,r*2.3f,tint=if(presence.file.endsWith(".svg"))mapColor(presence.faction) else null)
                        val model=presence.model
                        if(options.ships && model!=null && models.add(model)) {
                            if(presence.formation==3) listOf(Offset(-.6f,-2f),Offset(.6f,-2f),Offset(0f,-2.7f)).forEach { offset ->
                                icon(model,badge+offset*r,r*1.05f,r*.9f)
                            } else icon(model,badge+Offset(r,-r*2.2f),r*(if(model=="nave-automata")3.3f*1.03f else 2.7f),r*2.2f)
                        }
                    }
                }
                // Editorial Omicron landmarks are not live unit counts.
                if(options.ships && mapName(p)=="omicron") {icon("hive-lord",at+Offset(r*4f,-r*2.7f),r*3.5f);icon("draco-barata",at+Offset(r*4f,r*.1f),r*3.5f)}
                if(dssHost==p.index) icon(if(dssLive)"dss-operacional" else "dss-inoperante",at+Offset(0f,-r*4f),r*3.8f)
                if (p.regions.any { it.isAvailable == true } && (zoom >= 2.3f || selected == p.index)) {
                    circle(at + Offset(r * 2.3f, r * 1.8f), 2.dp.toPx() / unit, Color(0xFFB1C6CD))
                }
                if(selected==p.index) circle(at,r*3.4f,Color.White,1.3f/unit)
                if (detail) {
                    val gap = 17.sp.toPx() / unit
                    val caption = captions.getValue(p.index)
                    val progress = if (options.progress && (defense || offensive)) mapPercent(mapProgress(p)) else null
                    val lines = mutableListOf<MapCaption>()
                    if(options.names) lines += MapCaption(caption.first,at+Offset(0f,r*3.2f+gap),color,quiet)
                    if(options.players) lines += MapCaption(caption.second,at+Offset(0f,r*3.2f+gap*(if(options.names)2 else 1)),Color(0xFFADB7C5),true)
                    if(progress!=null) {
                        lines += MapCaption(progress,at+Offset(-r*6.4f,r*.9f),mapColor("human"),true)
                        if(defense) lines += MapCaption(mapPercent(mapInvasionProgress(p,now)),at+Offset(-r*6.4f,-r*1.8f),mapColor(mapFaction(p.event!!.faction)),true)
                    }
                    if(dssHost==p.index) lines += MapCaption(if(dssLive)"DSS" else "DSS · última posição",at+Offset(0f,-r*6.1f),Color(0xFFFFD23F),true)
                    val boxes = lines.map { line ->
                        textPaint.textSize = (if(line.small) 8.sp else 11.sp).toPx()
                        val half = textPaint.measureText(line.text)/2
                        val metrics = textPaint.fontMetrics
                        val margin = 3.dp.toPx()
                        val anchor = origin + line.at * unit
                        RectF(anchor.x-half-margin,anchor.y+metrics.top-margin,
                            anchor.x+half+margin,anchor.y+metrics.bottom+margin)
                    }
                    // Selection gets priority through drawOrder, without bypassing collision checks.
                    if (boxes.none { box -> occupied.any { RectF.intersects(it,box) } }) {
                        occupied.addAll(boxes)
                        pendingLabels.addAll(lines)
                    }
                }
            }
            canvas.restore()
            textPaint.setShadowLayer(2.dp.toPx(), 0f, 1.dp.toPx(), android.graphics.Color.BLACK)
            listOf(Triple("automaton",215f,45f),Triple("terminid",280f,45f),Triple("illuminate",113f,-46f)).forEach { (key,start,sweep) ->
                val path=Path().apply { addArc(RectF(origin.x-535f*unit,origin.y-535f*unit,origin.x+535f*unit,origin.y+535f*unit),start,sweep) }
                val title=when(key){"automaton"->"AUTÔMATOS";"terminid"->"TERMINÍDEOS";else->"ILUMINADOS"}
                textPaint.color=mapColor(key).toArgb();textPaint.textSize=14.sp.toPx();textPaint.textAlign=Paint.Align.LEFT
                val length=android.graphics.PathMeasure(path,false).length
                canvas.drawTextOnPath(title,path,(length-textPaint.measureText(title))/2f,0f,textPaint)
                textPaint.textAlign=Paint.Align.CENTER
            }

            // Text is rendered in screen pixels, outside the galaxy transform.
            textPaint.setShadowLayer(2.dp.toPx(), 0f, 1.dp.toPx(), android.graphics.Color.BLACK)
            pendingLabels.forEach { caption ->
                val anchor = origin + caption.at * unit
                textPaint.textSize = (if (caption.small) 8.sp else 11.sp).toPx()
                textPaint.color = caption.color.toArgb()
                textPaint.textAlign = Paint.Align.CENTER
                canvas.drawText(caption.text, anchor.x, anchor.y, textPaint)
            }
        }
        if (attacked.isNotEmpty() && options.motion && !stale) Canvas(Modifier.matchParentSize()) {
            val phase = alertPhase.value
            val glow = (.5f + .5f * sin(phase * 2f * PI.toFloat()))
            val unit = size.minDimension / 1120f * zoom
            val origin = center + pan
            if(invasions && routes) positioned.forEach { (source,a) ->
                source.attacking.forEach { id ->
                    val target=indexed[id]; val b=positions[id]
                    if(b!=null && target?.event!=null && mapFaction(source.currentOwner)==mapFaction(target.event.faction) &&
                        (routeFocus==null || source.index in routeFocus || id in routeFocus)) {
                        val at=origin+(a+(b-a)*phase)*unit
                        drawCircle(mapColor(mapFaction(source.currentOwner)).copy(alpha=.9f),2.dp.toPx(),at)
                    }
                }
            }
            attacked.forEach { (_, point) ->
                val at = origin + point * unit
                val radius = max(1000f / 260f * unit, 3.4.dp.toPx())
                drawCircle(Color(0xFFFF454D).copy(alpha = .06f + glow * .12f), radius * 3.25f, at)
                drawCircle(Color(0xFFFF454D).copy(alpha = .2f + glow * .38f),
                    radius * (3.1f + glow * .65f), at,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx()))
            }
        }
        Row(Modifier.align(Alignment.BottomEnd).padding(8.dp).background(Color(0xCC050810), RoundedCornerShape(10.dp))) {
            TextButton(onClick = { zoom = (zoom / 1.4f).coerceAtLeast(1f) }, modifier = Modifier.width(48.dp)) { Text("−") }
            TextButton(onClick = { zoom = 1f; pan = Offset.Zero }, modifier = Modifier.width(48.dp)) { Text("↺") }
            TextButton(onClick = { zoom = (zoom * 1.4f).coerceAtMost(10f) }, modifier = Modifier.width(48.dp)) { Text("+") }
        }
        if(art.done && art.failed > 0) Text("${art.failed} imagens indisponíveis", color=HD.Gold, fontSize=10.sp,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom=62.dp))
        if(positioned.isEmpty()) Text("Sem planetas neste filtro. Abra BUSCAR ou FILTROS.",color=HD.TextDim,fontSize=12.sp,
            modifier = Modifier.align(Alignment.Center).padding(24.dp))
    }
}
