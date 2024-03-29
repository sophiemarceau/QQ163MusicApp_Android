package com.sophiemarceauqu.lib_network.okhttp.listener;

public class DisposeDataHandle {
    public DisposeDataListener mListener = null;
    public Class<?> mClass = null;
    public String mSource = null;//文件保存路径

    public DisposeDataHandle(DisposeDataListener listener){
        this.mListener = listener;
    }

    public DisposeDataHandle(DisposeDataListener listener,Class<?> clazz){
        this.mListener = listener;
        this.mClass = clazz;
    }

    public DisposeDataHandle(DisposeDataListener listener,String mSource){
        this.mListener = listener;
        this.mSource = mSource;
    }
}
