package com.anonymous.ethervpn.model;

public class Server {

    private String country;
    private String flagUrl;
    private String ovpn;
    private String ovpnUserName;
    private String ovpnUserPassword;

    // v2 metadata — populated client-side; Firebase schema bump deferred
    private String isoCode = "";
    private String city = "";
    private int pingMs = -1;
    private int load = -1;
    private boolean premium = false;
    private boolean favorite = false;

    public Server(String country, String flagUrl, String ovpn, String ovpnUserName, String ovpnUserPassword) {
        this.country = country;
        this.flagUrl = flagUrl;
        this.ovpn = ovpn;
        this.ovpnUserName = ovpnUserName;
        this.ovpnUserPassword = ovpnUserPassword;
    }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getFlagUrl() { return flagUrl; }
    public void setFlagUrl(String flagUrl) { this.flagUrl = flagUrl; }

    public String getOvpn() { return ovpn; }
    public void setOvpn(String ovpn) { this.ovpn = ovpn; }

    public String getOvpnUserName() { return ovpnUserName; }
    public void setOvpnUserName(String ovpnUserName) { this.ovpnUserName = ovpnUserName; }

    public String getOvpnUserPassword() { return ovpnUserPassword; }
    public void setOvpnUserPassword(String ovpnUserPassword) { this.ovpnUserPassword = ovpnUserPassword; }

    public String getIsoCode() { return isoCode; }
    public void setIsoCode(String isoCode) { this.isoCode = isoCode; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public int getPingMs() { return pingMs; }
    public void setPingMs(int pingMs) { this.pingMs = pingMs; }

    public int getLoad() { return load; }
    public void setLoad(int load) { this.load = load; }

    public boolean isPremium() { return premium; }
    public void setPremium(boolean premium) { this.premium = premium; }

    public boolean isFavorite() { return favorite; }
    public void setFavorite(boolean favorite) { this.favorite = favorite; }
}
