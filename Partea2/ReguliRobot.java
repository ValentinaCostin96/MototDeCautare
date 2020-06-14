package proiect2;

public class ReguliRobot {
	
	public String userAgent; 
    public String regula;

    ReguliRobot() { }

    @Override public String toString()
    {
        StringBuilder rezultat = new StringBuilder();
        String Linie_Noua = System.getProperty("line.separator");
        rezultat.append(this.getClass().getName() + " Object {" + Linie_Noua);
        rezultat.append("   userAgent: " + this.userAgent + Linie_Noua);
        rezultat.append("   rule: " + this.regula + Linie_Noua);
        rezultat.append("}");
        return rezultat.toString();
    }

}
