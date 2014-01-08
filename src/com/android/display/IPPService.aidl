package com.android.display;
interface IPPService {
    boolean startPP();
    boolean stopPP();
    boolean updateHSIC(int h, int s, int i, int c);
    boolean getPPStatus();
}
