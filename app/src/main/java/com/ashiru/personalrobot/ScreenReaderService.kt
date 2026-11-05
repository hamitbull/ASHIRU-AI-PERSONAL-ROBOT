package com.ashiru.personalrobot

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class ScreenReaderService : AccessibilityService() {
    override fun onAccessibilityEvent(event:AccessibilityEvent?) {
        if(event==null || !requestNextRead) return
        if(event.packageName?.toString()==packageName) return
        val root=rootInActiveWindow ?: return
        val out=StringBuilder()
        collect(root,out,0)
        val captured=out.toString().trim().take(6000)
        if(captured.isNotBlank()){
            latestText=captured
            requestNextRead=false
        }
    }
    private fun collect(node:AccessibilityNodeInfo,out:StringBuilder,depth:Int) {
        if(depth>22 || out.length>6000) return
        val t=node.text?.toString()?.trim()
        if(!t.isNullOrEmpty() && out.indexOf(t)<0) out.append(t).append('\n')
        for(i in 0 until node.childCount){
            val child=node.getChild(i) ?: continue
            collect(child,out,depth+1)
            child.recycle()
        }
    }
    override fun onInterrupt(){requestNextRead=false}
    companion object {
        @Volatile var latestText:String?=null
        @Volatile var requestNextRead:Boolean=false
    }
}