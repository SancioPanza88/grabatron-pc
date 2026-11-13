package com.jirbo.adcolony;
/** I video pubblicitari non esistono piu': nessun video disponibile. */
public class AdColonyVideoAd {
    public AdColonyVideoAd() {}
    public AdColonyVideoAd(String zone) {}
    public boolean getV4VCAvailable() { return false; }
    public boolean isReady() { return false; }
    public boolean show(AdColonyVideoListener l) { return false; }
    public boolean showV4VC(AdColonyVideoListener l) { return false; }
    public boolean showV4VC(AdColonyVideoListener l, boolean b) { return false; }
}
