package com.clj.fastble.server

import android.os.Handler
import android.os.Message
import com.clj.fastble.IConnectionControl
import com.ubtech.utilcode.utils.LogUtils
import java.util.*

/**
 * @Deseription 心跳策略
 * @Author tanghongyu
 * @Time 2018/6/8 16:05
 */
class HeartBeatStrategy {


    lateinit var iBleControl: IConnectionControl
    private var isNeedHeartBeat : Boolean = true

    @Volatile private var failCount = 0
    companion object {
        val instance: HeartBeatStrategy by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            HeartBeatStrategy() }
    }

    fun init(iBleControl: IConnectionControl) {
        LogUtils.d("init")
//        this.iBleControl = iBleControl
    }

    @Synchronized
    fun updateTimeCount() {
        LogUtils.d("updateTimeCount")
//        failCount = 0
//        isNeedHeartBeat = false
    }
    lateinit var timer : Timer
    fun startHeartBeat() {
        LogUtils.d("startHeartBeat")
//        timer = Timer()
//        timer.schedule(object : TimerTask() {
//            override fun run() {
//                synchronized(this@HeartBeatStrategy) {
//                    handler.sendEmptyMessage(1)
//
//                    if(isNeedHeartBeat) {
//                        LogUtils.d("sendHeartBeat")
//                        if(iBleControl != null)iBleControl.sendHeartBeat(object : IConnectionControl.IHeartCallback{
//                            override fun onSuccess() {
//                                updateTimeCount()
//                            }
//
//                            override fun onFail() {
//                                LogUtils.w("sendHeartBeat fail!")
//                            }
//
//                        })
//                    }else{
//                        LogUtils.i("ignore HeartBeat")
//                        isNeedHeartBeat = true
//                    }
//                }
//
//
//
//
//
//            }
//
//        },1000,2000 )
    }
    fun stopHeartBeat() {
//        timer.cancel()
    }
    var handler = object : Handler() {
        override fun handleMessage(msg: Message?) {
            super.handleMessage(msg)
            if(msg!!.what == 1 ) {
                if(failCount == 9) {
                    iBleControl.disconnect()
                    LogUtils.e("failCount > 3")
                    failCount = 0
                    stopHeartBeat()
                }
                failCount++

            }
        }
    }
}