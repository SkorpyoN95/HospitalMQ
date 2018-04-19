public enum Injury {
    KNEE, ELBOW, HIP;

    @Override
    public String toString() {
        switch(this){
            case KNEE: return "knee";
            case ELBOW: return  "elbow";
            case HIP: return "hip";
            default: return "UNKNOWN";
        }
    }
}
