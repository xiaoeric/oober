package com.oober.oober;

import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class CompositeOnClickListener implements View.OnClickListener{
    List<View.OnClickListener> listeners;

    public CompositeOnClickListener(){
        listeners = new ArrayList<View.OnClickListener>();
    }

    public void addOnClickListener(View.OnClickListener listener){
        listeners.add(listener);
    }

    @Override
    public void onClick(View v){
        for(View.OnClickListener listener : listeners){
            listener.onClick(v);
        }
    }
}