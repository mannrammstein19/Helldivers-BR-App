package br.com.helldiversbr.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import br.com.helldiversbr.app.ui.theme.HD
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlin.math.sin

/** Local click target only; never installs a swipe handler competing with the drawer. */
@Composable
fun DssMapDock(expanded: Boolean, model: String?, status: String, stale: Boolean, motion: Boolean,
               onToggle: () -> Unit, onDetails: () -> Unit, modifier: Modifier = Modifier) {
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    val opacity = produceState(1f, motion, lifecycle) {
        value = 1f
        if(motion) lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            val start=android.os.SystemClock.elapsedRealtime()
            while(true) {
                val seconds=(android.os.SystemClock.elapsedRealtime()-start)/1000f
                value=.88f+.12f*sin(seconds*1.57f)
                delay(80)
            }
        }
    }
    Surface(modifier=modifier, color=Color(0xF0090D12), shape=RoundedCornerShape(13.dp),
        border=BorderStroke(1.dp,HD.Border)) {
        Row(verticalAlignment=Alignment.CenterVertically) {
            AnimatedVisibility(expanded, enter=expandHorizontally(animationSpec=tween(if(motion) 220 else 0),expandFrom=Alignment.End)+fadeIn(tween(if(motion) 180 else 0)),
                exit=shrinkHorizontally(animationSpec=tween(if(motion) 220 else 0),shrinkTowards=Alignment.End)+fadeOut(tween(if(motion) 150 else 0)),
                modifier=Modifier.weight(1f, fill=false)) {
                Column(Modifier.widthIn(max=266.dp).clickable(onClick=onDetails)
                    .semantics { contentDescription="DSS. $status. Abrir detalhes" }.padding(start=12.dp,end=6.dp,top=8.dp,bottom=8.dp)) {
                    Text("DSS · Estação Espacial da Democracia",color=HD.Yellow,fontWeight=FontWeight.Bold,fontSize=10.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
                    Text(status,color=if(stale) HD.Gold else HD.Text,fontSize=11.sp,maxLines=2,overflow=TextOverflow.Ellipsis)
                }
            }
            Box(Modifier.size(64.dp).clickable(onClick=onToggle)
                .semantics { contentDescription=if(expanded) "Recolher aviso da DSS" else "Mostrar aviso da DSS" },contentAlignment=Alignment.Center) {
                AsyncImage(model,contentDescription=null,modifier=Modifier.size(52.dp).graphicsLayer { alpha=opacity.value })
            }
        }
    }
}
