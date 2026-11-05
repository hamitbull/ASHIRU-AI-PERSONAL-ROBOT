package com.ashiru.personalrobot

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class ScreenReaderService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if(event==null) return
        val root=rootInActiveWindow ?: return
        val out=StringBuilder()
        collect(root,out,0)
        latestText=out.toString().trim().take(8000)
    }
    private fun collect(node:AccessibilityNodeInfo,out:StringBuilder,depth:Int) {
        if(depth>25 || out.length>8000) return
        val t=node.text?.toString()?.trim()
        if(!t.isNullOrEmpty() && out.indexOf(t)<0) out.append(t).append('\n')
        for(i in 0 until node.childCount) node.getChild(i)?.let { child -> collect(child,out,depth+1); child.recycle() }
    }
    override fun onInterrupt() {}
    companion object { @Volatile var latestText:String?=null }
}