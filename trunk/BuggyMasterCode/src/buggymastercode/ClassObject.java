/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package buggymastercode;

/**
 *
 * @author jalvarez
 */
public class ClassObject {

    private int m_id = 0;
    private String m_package = "";
    private String m_vbName = "";
    private String m_javaName = "";

    public void setId(int value) {m_id = value;}
    public int getId() {return m_id;}
    public void setPackage(String value) {m_package = value;}
    public void setVbName(String value) {m_vbName = value;}
    public void setJavaName(String value) {m_javaName = value;}

    public boolean saveClass() {
        if (m_id == Db.CS_NO_ID) {

            DataBaseId id = new DataBaseId();

            if (!Db.db.getNewId("tclass", id)) {
                return false;
            }

            String sqlstmt = "insert into tclass (cl_id, cl_vbname, cl_javaname, cl_package) values ("
                            + id.getId().toString()
                            + ", " + Db.getString(m_package)
                            + ", " + Db.getString(m_vbName)
                            + ", " + Db.getString(m_javaName)
                            + ")";

            if (Db.db.execute(sqlstmt)) {
                m_id = id.getId();
            }
            else {
                return false;
            }

        }
        else {

            String sqlstmt = "update tclass set "
                            + "cl_package = "  + Db.getString(m_package)
                            + ", cl_vbname = "  + Db.getString(m_vbName)
                            + ", cl_javaname = "  + Db.getString(m_javaName)
                            + " where cl_id = " + ((Integer)m_id).toString();

            if (!Db.db.execute(sqlstmt)) {
                return false;
            }
        }
        return true;
    }

    public boolean deleteClass() {
        String sqlstmt = "delete from tclass where cl_id = " + ((Integer)m_id).toString();
        if (Db.db.execute(sqlstmt)) {
            return true;
        }
        else {
            return false;
        }
    }
}
