// Decompiled by Jad v1.5.8e. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/kpdus/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   MessageEvent.java

package net.btrace.flightrecorder;

import com.oracle.jrockit.jfr.EventDefinition;
import com.oracle.jrockit.jfr.EventToken;
import com.oracle.jrockit.jfr.InstantEvent;
import com.oracle.jrockit.jfr.ValueDefinition;

@EventDefinition(name="btrace-log", path="BTrace/Sample", description="Message generated by BTrace  ")
public class MessageEvent extends InstantEvent
{

    public MessageEvent(EventToken eventToken, String message) throws IllegalArgumentException {
        super(eventToken);
        this.message = message;
    }

    @ValueDefinition(name="message")
    private String message;

    public String getMessage() {
        return message;
    }
}
