package br.com.helldiversbr.app.ui.screens

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
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

// The original blue sprite keeps its luminance-derived transparency.
private val TCS_BLUE_SMOKE = android.graphics.ColorMatrixColorFilter(floatArrayOf(
    .064f,.215f,.021f,0f,0f, .162f,.544f,.055f,0f,0f,
    .213f,.715f,.072f,0f,0f, .213f,.715f,.072f,0f,0f))

// Blend a warning filter with the original artwork, preserving texture and alpha.
private fun tcsWarningFilters(color: Color): List<android.graphics.ColorMatrixColorFilter> =
    (0..15).map { step ->
        android.graphics.ColorMatrixColorFilter(tcsWarningMatrix(color.red, color.green, color.blue, step / 15f * .72f))
    }
private val TCS_ATTACK_FILTERS = tcsWarningFilters(Color(0xFFFFC34D))
private val TCS_LOSS_FILTER = tcsWarningFilters(Color(0xFFFF524D)).last()

private const val EARTH_MAP_SCALE = 1.9f * 1.05f

private data class MapSpecialMarker(val point: Offset, val key: String, val name: String)

private data class MapRoute(val a: Offset, val b: Offset, val from: Planet, val to: Planet)

private data class MapCaption(val text: String, val at: Offset, val color: Color, val small: Boolean = false, val bold: Boolean = false)

private data class MapShip(val planet: Planet, val point: Offset, val key: String, val offset: Offset, val width: Float, val height: Float)

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
        val files = MapAssets.all().filterKeys { it in listOf("human", "automaton", "terminid", "illuminate", "earth", "defense", "liberation", "galaxy", "penta", "meridia", "wreckage", "hive-lord", "draco-barata", "tcs-plus", "tcs-pulse", "cyberstan-pulse", "dss-operacional", "dss-inoperante") || it.startsWith("nave-") || it.startsWith("imagens/guerra/presencas/") } +
            MapAssets.planetEntries().mapKeys { "planet:${it.key}" }
        val semaphore = Semaphore(8)
        val loaded = coroutineScope {
            files.map { (key, file) -> async { semaphore.withPermit {
                val result = context.imageLoader.execute(ImageRequest.Builder(context).data("file:///android_asset/$file")
                    .size(if (key == "tcs-pulse") 3840 else if (key == "cyberstan-pulse") 1536 else if (key == "galaxy") 1200 else if(key.startsWith("planet:")) 96 else 192).allowHardware(false).build())
                key to if(result is SuccessResult) result.drawable.toBitmap() else null
            } } }.awaitAll()
        }
        value = MapArt(loaded.mapNotNull { (key, bitmap) -> bitmap?.let { key to it } }.toMap(), loaded.count { it.second == null }, true)
    }
    val gifs by produceState<Map<String, MapGifSprite>>(emptyMap(), context) {
        value = withContext(Dispatchers.IO) {
            listOf("penta", "meridia").mapNotNull { key ->
                MapGifSprite.load(context.assets, "map/static/imagens/planetas/$key.gif")?.let { key to it }
            }.toMap()
        }
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
    val specialMarkers = remember(positioned) {
        positioned.mapNotNull { (planet, point) -> mapSpecial(planet)?.let { MapSpecialMarker(point, it, mapName(planet)) } }
    }
    val infrastructure = remember(positioned) { positioned.filter { br.com.helldiversbr.app.data.TcsInfrastructure.has(it.first) && mapSpecial(it.first) == null } }
    val attacked = remember(positioned) { positioned.filter { isPlanetUnderAttack(it.first) } }
    val attacks = remember(all, active) { mapAttackLinks(all, active) }
    val ships = remember(positioned, options.presences, options.ships) {
        if (!options.presences || !options.ships) emptyList() else positioned.flatMap { (p, point) ->
            val seen = mutableSetOf<String>()
            PlanetPresences.list(p).take(3).flatMapIndexed { i, presence ->
                val model = presence.model
                if (model == null || !seen.add(model)) emptyList() else {
                    val badge = Offset(3f, i * 3f - .4f)
                    if (presence.formation == 3) listOf(Offset(-.6f,-2f), Offset(.6f,-2f), Offset(0f,-2.7f)).map {
                        MapShip(p, point, model, badge + it, 1.05f, .9f)
                    } else listOf(MapShip(p, point, model, badge + Offset(1f,-2.2f),
                        if(model == "nave-automata") 3.3f * 1.03f else 2.7f, 2.2f))
                }
            }
        }
    }
    // Visual motion is independent of telemetry freshness and refresh requests.
    // Saved readings still retain their timestamps and do not become live data.
    val animated = options.motion
    val animationClock = remember { MapAnimationClock() }
    // Read the clock only in the small overlay; static geography never follows it.
    val animationSeconds = produceState(0f, animated, lifecycle) {
        if (animated) lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
            while (true) {
                withFrameNanos { frameTime -> value = animationClock.secondsAt(frameTime) }
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
    // Use the complete reading so faction/search filters cannot hide a real border.
    val frontier = remember(all) {
        br.com.helldiversbr.app.data.enemyBorderPlanets(all.filter { mapFaction(it.currentOwner) != "unknown" }.map {
            br.com.helldiversbr.app.data.MapBorderNode(it.index, mapFaction(it.currentOwner) == "human", it.waypoints)
        })
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
            fun icon(key: String, at: Offset, width: Float, height: Float = width, alpha: Int = 255, tint: Color? = null, filter: android.graphics.ColorFilter? = null): Boolean {
                val image = art.bitmaps[key] ?: return false
                paint.shader = null; paint.style = Paint.Style.FILL; paint.color = android.graphics.Color.WHITE
                paint.alpha = alpha
                paint.colorFilter = filter ?: tint?.let { android.graphics.PorterDuffColorFilter(it.toArgb(), android.graphics.PorterDuff.Mode.SRC_IN) }
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
            canvas.restore()
            // TCS light is below routes, planets and labels; luminance supplies alpha.
            if (animated && options.infrastructure && !options.clean) art.bitmaps["tcs-pulse"]?.let { bitmap ->
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    isFilterBitmap = true
                    alpha = 155
                }
                infrastructure.forEach { (planet, point) ->
                    val status = br.com.helldiversbr.app.data.TcsInfrastructure.state(planet)
                    if (status == br.com.helldiversbr.app.data.TcsState.ALLIED || status == br.com.helldiversbr.app.data.TcsState.ATTACKED) {
                        paint.colorFilter = TCS_BLUE_SMOKE
                        // Historical references stay visible without implying a newly confirmed loss.
                        paint.alpha = if (stale || planet.presenceHistoryStale || planet.savedTcsPresent) 95 else 155
                        val frame = br.com.helldiversbr.app.data.TcsInfrastructure.frame(planet.index, (animationSeconds.value * 1000).toLong())
                        val width = bitmap.width / 40
                        val at = origin + point * unit
                        val radius = max(1000f / 260f * unit, 1.8.dp.toPx()) * 4f
                        if (at.x in -radius..(size.width+radius) && at.y in -radius..(size.height+radius)) {
                            val canvas = drawContext.canvas.nativeCanvas
                            canvas.save()
                            canvas.clipPath(Path().apply { addCircle(at.x, at.y, radius, Path.Direction.CW) })
                            canvas.drawBitmap(bitmap, android.graphics.Rect(frame*width,0,(frame+1)*width,bitmap.height),
                                RectF(at.x-radius,at.y-radius,at.x+radius,at.y+radius), paint)
                            canvas.restore()
                        }
                    }
                }
            }
            // Cyberstan's local sprite stays underneath routes and planet artwork.
            if (!options.clean) art.bitmaps["cyberstan-pulse"]?.let { bitmap ->
                val pulse = br.com.helldiversbr.app.data.CyberstanPulse
                val frame = pulse.frame((animationSeconds.value * 1000).toLong(), animated)
                val cell = bitmap.width / pulse.COLUMNS
                val left = frame % pulse.COLUMNS * cell
                val top = frame / pulse.COLUMNS * cell
                positioned.firstOrNull { mapName(it.first) == "cyberstan" }?.let { (_, point) ->
                    val at = origin + point * unit
                    val radius = max(1000f / 260f * unit, 1.8.dp.toPx()) * 5f
                    if (at.x in -radius..(size.width + radius) && at.y in -radius..(size.height + radius)) {
                        paint.shader = null; paint.colorFilter = null; paint.alpha = 180
                        canvas.drawBitmap(bitmap, android.graphics.Rect(left, top, left + cell, top + cell),
                            RectF(at.x - radius, at.y - radius, at.x + radius, at.y + radius), paint)
                        paint.alpha = 255
                    }
                }
            }
            canvas.save(); canvas.translate(origin.x, origin.y); canvas.scale(unit, unit)
            if (routes) edges.forEach { (a, b, source, target) ->
                val sa = origin + a * unit; val sb = origin + b * unit
                if (max(sa.x,sb.x) >= 0 && min(sa.x,sb.x) <= size.width &&
                    max(sa.y,sb.y) >= 0 && min(sa.y,sb.y) <= size.height) {
                    paint.style = Paint.Style.STROKE; paint.strokeWidth = 3.dp.toPx() / unit
                    val ca = mapRouteColor(source.currentOwner).toArgb()
                    val cb = mapRouteColor(target.currentOwner).toArgb()
                    paint.shader = android.graphics.LinearGradient(a.x,a.y,b.x,b.y,intArrayOf(ca,ca,cb,cb),floatArrayOf(0f,.4f,.6f,1f),android.graphics.Shader.TileMode.CLAMP)
                    canvas.drawLine(a.x,a.y,b.x,b.y,paint); paint.shader = null
                }
            }
            drawOrder.forEach { (p,at) ->
                val screen = origin + at * unit
                if(screen.x < -100 || screen.y < -100 || screen.x > size.width+100 || screen.y > size.height+100) return@forEach
                val special=mapSpecial(p);val capital=mapEarth(p);val key=mapFaction(p.currentOwner)
                val defense=special==null&&p.event!=null;val offensive=mapShowsOffensiveProgress(p,active)
                val r = max(1000f / 260f, (if (defense || offensive) 3.4.dp else 1.8.dp).toPx() / unit) * (if (capital) EARTH_MAP_SCALE else 1f)
                val quiet = key == "human" && !defense && !offensive && selected != p.index
                val color = mapOwnerColor(p.currentOwner)
                val detail = selected == p.index || zoom >= (if (quiet) 5f else if (defense || offensive) 1.6f else 2.3f)
                if(capital) listOf(4f,3f,2.2f).forEachIndexed { i,v->circle(at,r*v,Color(0xFFFFE68A).copy(alpha=.025f+i*.015f)) }
                if(special==null && mapName(p) != "cyberstan" && (p.index in frontier || defense || offensive)) circle(at,r*(if(defense||offensive)2.7f else 2.1f),color.copy(alpha=.12f))
                if(defense||offensive) arc(at,r*2.08f,mapProgress(p),mapColor("human"),r*.48f)
                if(offensive) arc(at,r*2.72f,100.0-(mapProgress(p) ?: 0.0),color,r*.32f)
                if(defense) arc(at,r*2.72f,mapInvasionProgress(p,now),mapColor(mapFaction(p.event!!.faction)),r*.32f)
                val dotR=if(defense||offensive)r*1.38f else r
                paint.style=Paint.Style.FILL
                paint.shader = null; paint.color = color.toArgb()
                if(special==null) canvas.drawCircle(at.x, at.y, dotR, paint)
                val iconSize=r*(if(key=="human")2f else 1.55f)
                if(capital) { if(!icon("earth",at,r*2)) icon("human",at,iconSize) }
                else if(special==null) {
                    canvas.save()
                    canvas.clipPath(Path().apply { addCircle(at.x, at.y, dotR, Path.Direction.CW) })
                    if(!icon("planet:${p.index}",at,dotR*2)) icon(if(key=="unknown")"human" else key,at,iconSize)
                    canvas.restore()
                }
                if(special==null) {
                    circle(at,dotR,Color(0xFF080C12),.6.dp.toPx()/unit)
                    circle(at,dotR+1.dp.toPx()/unit,if(key == "human") mapColor("human") else color,1.5.dp.toPx()/unit)
                }
                if(special==null && !defense && key !in listOf("human","unknown") && !options.clean) icon(key,at+Offset(0f,-r*2.3f),r*1.6f)
                if(options.progress && (defense||offensive)) {
                    val humanBadge = at+Offset(-r*3.8f,r*.9f)
                    icon(if(defense) "defense" else "liberation",humanBadge,r*1.8f)
                    if(defense) icon(mapFaction(p.event!!.faction),at+Offset(-r*3.8f,-r*1.8f),r*1.8f)
                }
                if (options.infrastructure && br.com.helldiversbr.app.data.TcsInfrastructure.has(p) && special == null) {
                    val badge = at + Offset(0f, -r * 3.2f)
                    paint.shader = null; paint.style = Paint.Style.STROKE
                    paint.color = tcsStateColor(br.com.helldiversbr.app.data.TcsInfrastructure.state(p)).copy(alpha = .85f).toArgb()
                    paint.strokeWidth = 1.2.dp.toPx() / unit
                    canvas.drawLine(at.x, at.y - dotR, badge.x, badge.y + r * .95f, paint)
                    canvas.drawLine(at.x, at.y - dotR, at.x - r * .3f, at.y - dotR - r * .45f, paint)
                    canvas.drawLine(at.x, at.y - dotR, at.x + r * .3f, at.y - dotR - r * .45f, paint)
                    val warning = when (br.com.helldiversbr.app.data.TcsInfrastructure.state(p)) {
                        br.com.helldiversbr.app.data.TcsState.ATTACKED -> {
                            val step = if (animated) (((sin(animationSeconds.value * 4.0) + 1) * 7.5).toInt()).coerceIn(0,15) else 8
                            TCS_ATTACK_FILTERS[step]
                        }
                        br.com.helldiversbr.app.data.TcsState.COMPROMISED -> TCS_LOSS_FILTER
                        else -> null
                    }
                    icon("tcs-plus", badge, r * 1.9f, filter = warning)
                }
                if(options.presences) {

                    PlanetPresences.list(p).take(3).forEachIndexed { i, presence ->
                        val badge=at+Offset(r*3f, r*(i*3f-.4f))
                        paint.style=Paint.Style.STROKE; paint.color=mapColor(presence.faction).copy(alpha=.85f).toArgb(); paint.strokeWidth=1.2.dp.toPx()/unit
                        canvas.drawLine(at.x+dotR,at.y,badge.x-r,badge.y,paint)
                        icon("imagens/guerra/presencas/${presence.file}",badge,r*2.3f,tint=if(presence.file.endsWith(".svg"))mapColor(presence.faction) else null)

                    }
                }
                // Editorial Omicron landmarks are not live unit counts.
                if(options.ships && mapName(p)=="omicron") {icon("hive-lord",at+Offset(r*4f,-r*2.7f),r*3.5f);icon("draco-barata",at+Offset(r*4f,r*.1f),r*3.5f)}
                if(dssHost==p.index) icon(if(dssLive)"dss-operacional" else "dss-inoperante",at+Offset(0f,-r*4f),r*5.0f)
                if (p.regions.any { it.isAvailable == true } && (zoom >= 2.3f || selected == p.index)) {
                    circle(at + Offset(r * 2.3f, r * 1.8f), 2.dp.toPx() / unit, Color(0xFFB1C6CD))
                }
                if(selected==p.index && special==null) circle(at,r*3.4f,Color.White,1.3f/unit)
                if (detail) {
                    val caption = captions.getValue(p.index)
                    val progress = if (options.progress && (defense || offensive)) mapPercent(mapProgress(p)) else null
                    val lines = mutableListOf<MapCaption>()
                    textPaint.typeface = android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.BOLD)
                    textPaint.textSize = (if(quiet) 10.sp else 12.sp).toPx()
                    val nameMetrics = textPaint.fontMetrics
                    textPaint.textSize = 9.sp.toPx()
                    val countMetrics = textPaint.fontMetrics
                    val top = if (special != null) {
                        r * unit * when (special) { "penta" -> 1.9f; "meridia" -> 1.7f; else -> 2.9f } + 2.dp.toPx()
                    } else mapLabelTop(r*unit, defense, offensive, 4.dp.toPx(), false, selected==p.index) + if(defense || offensive) 0f else 1.dp.toPx()
                    val (nameBaseline, countBaseline) = mapLabelBaselines(top, nameMetrics.ascent, nameMetrics.descent,
                        countMetrics.ascent, options.names, 1.dp.toPx())
                    val adjustedNameBaseline = if (special == "wreckage") nameBaseline - r * unit * 1.55f else nameBaseline
                    if(options.names) lines += MapCaption(caption.first,at+Offset(0f,adjustedNameBaseline/unit),mapNameColor(p.currentOwner),quiet,true)
                    if(options.players) lines += MapCaption(caption.second,at+Offset(0f,countBaseline/unit),Color(0xFFADB7C5),true)
                    if(progress!=null) {
                        lines += MapCaption(progress,at+Offset(-r*6.4f,r*.9f),mapColor("human"),true)
                        if(defense) lines += MapCaption(mapPercent(mapInvasionProgress(p,now)),at+Offset(-r*6.4f,-r*1.8f),mapColor(mapFaction(p.event!!.faction)),true)
                    }
                    if(dssHost==p.index) lines += MapCaption(if(dssLive)"DSS" else "DSS · última posição",at+Offset(0f,-r*7f),Color(0xFFFFD23F),true)
                    val boxes = lines.map { line ->
                        textPaint.typeface = android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.BOLD)
                        textPaint.textSize = (if(line.bold) { if(line.small) 10.sp else 12.sp } else 9.sp).toPx()
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
            textPaint.clearShadowLayer()
            pendingLabels.forEach { caption ->
                val anchor = origin + caption.at * unit
                textPaint.typeface = android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.BOLD)
                textPaint.textSize = (if(caption.bold) { if(caption.small) 10.sp else 12.sp } else 9.sp).toPx()
                textPaint.textAlign = Paint.Align.CENTER
                textPaint.style = Paint.Style.STROKE
                textPaint.strokeWidth = 2.5.dp.toPx()
                textPaint.strokeJoin = Paint.Join.ROUND
                textPaint.color = android.graphics.Color.BLACK
                canvas.drawText(caption.text, anchor.x, anchor.y, textPaint)
                textPaint.style = Paint.Style.FILL
                textPaint.color = caption.color.toArgb()
                canvas.drawText(caption.text, anchor.x, anchor.y, textPaint)
            }
        }
        // Transparent, non-interactive overlay: special artwork and ships remain visible with motion disabled.
        Canvas(Modifier.matchParentSize()) {
            val seconds = animationSeconds.value
            val unit = size.minDimension / 1120f * zoom
            val origin = center + pan
            if (animated && invasions && routes) attacks.forEach { link ->
                val a = positions[link.source]; val b = positions[link.target]
                if (a != null && b != null && (routeFocus == null || link.source in routeFocus || link.target in routeFocus)) {
                    val sa = origin + a * unit; val sb = origin + b * unit
                    val delta = sb - sa; val length = delta.getDistance()
                    val padding = max(1000f / 260f * unit * 2.8f, 12.dp.toPx())
                    if (length > padding * 2f) {
                        val direction = delta / length
                        val start = sa + direction * padding
                        val usable = length - padding * 2f
                        val phase = (seconds % 2.6f) / 2.6f
                        val tail = start + direction * usable * max(0f, phase - .12f)
                        val head = start + direction * usable * phase
                        val energy = when(link.faction) {
                            "human" -> Color(0xFFC1EFFF)
                            "automaton" -> Color(0xFFFFB5B5)
                            "terminid" -> Color(0xFFFFE1A6)
                            else -> Color(0xFFDDC2FF)
                        }
                        drawLine(energy.copy(alpha = .85f), tail, head, 3.dp.toPx(), androidx.compose.ui.graphics.StrokeCap.Round)
                    }
                }
            }
            val spritePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
            // Supplied black-hole artwork has an opaque black background. SCREEN
            // blends that black into the map while retaining the luminous rings.
            val blackHolePaint = Paint(spritePaint).apply {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
            }
            specialMarkers.forEach { marker ->
                val radius = max(1000f / 260f * unit, 1.8.dp.toPx())
                val at = origin + marker.point * unit
                val margin = radius * 4f
                if(at.x in -margin..(size.width+margin) && at.y in -margin..(size.height+margin)) {
                    val gif = gifs[marker.key]
                    val artwork = gif?.frameAt(seconds, animated) ?: art.bitmaps[marker.key]
                    artwork?.let { bitmap ->
                        // Black holes play their original GIF frames; only wreckage rotates.
                        val angle = if (animated && marker.key == "wreckage") when (marker.name) {
                            "ivis" -> -(seconds % 100f) / 100f * 360f
                            "moradesh" -> (seconds % 90f) / 90f * 360f
                            else -> sin(seconds * 2f * PI.toFloat() / 18f) * 22f
                        } else 0f
                        val diameter = radius * when (marker.key) { "penta" -> 9.6f; "meridia" -> 8.4f; else -> 4f }
                        val ratio = min(diameter / bitmap.width, diameter / bitmap.height)
                        val w = bitmap.width * ratio; val h = bitmap.height * ratio
                        val canvas = drawContext.canvas.nativeCanvas
                        canvas.save()
                        canvas.rotate(angle, at.x, at.y)
                        val paint = if (marker.key == "penta" || marker.key == "meridia") blackHolePaint else spritePaint
                        canvas.drawBitmap(bitmap, null, RectF(at.x-w/2,at.y-h/2,at.x+w/2,at.y+h/2), paint)
                        canvas.restore()
                    }
                }
            }
            ships.forEach { ship ->
                val p = ship.planet
                val offensive = mapShowsOffensiveProgress(p, active)
                val radius = max(1000f / 260f * unit, (if(p.event != null || offensive) 3.4.dp else 1.8.dp).toPx()) * (if(mapEarth(p)) EARTH_MAP_SCALE else 1f)
                val float = if(animated) sin(seconds * 2f * PI.toFloat() / 6f + (p.index % 11).toFloat()) * 1.5.dp.toPx() else 0f
                val at = origin + ship.point * unit + ship.offset * radius + Offset(0f, float)
                if (at.x in -100f..(size.width+100f) && at.y in -100f..(size.height+100f)) art.bitmaps[ship.key]?.let { bitmap ->
                    val ratio = min(ship.width * radius / bitmap.width, ship.height * radius / bitmap.height)
                    val w = bitmap.width * ratio; val h = bitmap.height * ratio
                    drawContext.canvas.nativeCanvas.drawBitmap(bitmap, null, RectF(at.x-w/2,at.y-h/2,at.x+w/2,at.y+h/2), spritePaint)
                }
            }
            if (animated) attacked.forEach { (_, point) ->
                val phase = (seconds % 1.8f) / 1.8f
                val glow = .5f + .5f * sin(phase * 2f * PI.toFloat())
                val at = origin + point * unit
                val radius = max(1000f / 260f * unit, 3.4.dp.toPx())
                drawCircle(Color(0xFFFF454D).copy(alpha = .06f + glow * .12f), radius * 3.25f, at)
                drawCircle(Color(0xFFFF454D).copy(alpha = .2f + glow * .38f), radius * (3.1f + glow * .65f), at,
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
