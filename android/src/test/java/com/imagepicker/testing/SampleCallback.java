package com.imagepicker.testing;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.JavaOnlyMap;

/**
 * Created by rusfearuth on 10.04.17.
 */

public class SampleCallback implements Callback
{
    private boolean hasError;
    private boolean didCancel;

    @Override
    public void invoke(Object... args)
    {
        System.out.println(args.length);
        System.out.println(String.valueOf(args[0]));
        System.out.println(args[0].getClass());
        for (int i = 0; i < args.length; i++) {
            if (lookingForError(args[i])) {
                break;
            }
        }
        for (int i = 0; i < args.length; i++) {
            if (lookingForCancelation(args[i])) {
                break;
            }
        }
    }

    public boolean hasError()
    {
        return hasError;
    }

    public boolean didCancel()
    {
        return didCancel;
    }

    private boolean lookingForError(Object arg)
    {
        hasError = false;
        if (arg == null)
        {
            return hasError;
        }

        if (arg instanceof String)
        {
            hasError = arg.equals("error") || ((String) arg).contains("error");
        }
        else if (arg instanceof JavaOnlyMap)
        {
            hasError = ((JavaOnlyMap) arg).hasKey("error");
        }
        return hasError;
    }

    private boolean lookingForCancelation(Object arg)
    {
        didCancel = false;
        if (arg == null)
        {
            return didCancel;
        }

        if (arg instanceof String)
        {
            didCancel = arg.equals("didCancel") || ((String) arg).contains("didCancel");
        }
        else if (arg instanceof JavaOnlyMap)
        {
            didCancel = ((JavaOnlyMap) arg).hasKey("didCancel");
        }
        return didCancel;
    }
}
