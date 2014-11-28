package com.android.location.XT;

import com.android.location.XT.IXTSrvCb;

interface IXTSrv
{
    boolean disable();
    boolean getStatus();
    String  getText(int which);
    void showDialog();
    void registerCallback(IXTSrvCb cb);
    void unregisterCallback(IXTSrvCb cb);
}
