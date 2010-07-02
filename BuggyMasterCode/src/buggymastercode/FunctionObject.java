/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package buggymastercode;

/**
 *
 * @author jalvarez
 */
public class FunctionObject {

    private int m_id = 0;
    private int m_cl_id = 0;
    private String m_vbName = "";
    private String m_javaName = "";
    private String m_dataType = "";

    public void setId(int value) {m_id = value;}
    public int getId() {return m_id;}
    public void setClId(int value) {m_cl_id = value;}
    public void setVbName(String value) {m_vbName = value;}
    public void setJavaName(String value) {m_javaName = value;}
    public void setDataType(String value) {m_dataType = value;}

    public boolean saveFunction() {
        if (m_id == Db.CS_NO_ID) {

            DataBaseId id = new DataBaseId();

            if (!Db.db.getNewId("tfunction", id)) {
                return false;
            }

            String sqlstmt = "insert into tfunction (cl_id, fun_id, fun_vbname, fun_javaname, fun_datatype) values ("
                            + ((Integer)m_cl_id).toString()
                            + ", " + id.getId().toString()
                            + ", " + Db.getString(m_vbName)
                            + ", " + Db.getString(m_javaName)
                            + ", " + Db.getString(m_dataType)
                            + ")";

            if (Db.db.execute(sqlstmt)) {
                m_id = id.getId();
            }
            else {
                return false;
            }

        }
        else {

            String sqlstmt = "update tfunction set "
                            + "fun_vbname = "  + Db.getString(m_vbName)
                            + ", fun_javaname = "  + Db.getString(m_javaName)
                            + ", fun_datatype = "  + Db.getString(m_dataType)
                            + " where fun_id = " + ((Integer)m_id).toString();

            if (!Db.db.execute(sqlstmt)) {
                return false;
            }
        }
        return true;
    }

    public boolean deleteFunction() {
        String sqlstmt = "delete from tfunction where fun_id = " + ((Integer)m_id).toString();
        if (Db.db.execute(sqlstmt)) {
            return true;
        }
        else {
            return false;
        }
    }

    public boolean getFunctionIdFromFunctionName() {
        G.setHourglass();
        setId(Db.CS_NO_ID);
        String sqlstmt = "select fun_id from tfunction"
                            + " where fun_vbname = " + Db.getString(m_vbName)
                            + " and cl_id = " + Integer.toString(m_cl_id);
        DBRecordSet rs = new DBRecordSet();
        if (!Db.db.openRs(sqlstmt, rs)) {
            G.setDefaultCursor();
            return false;
        }

        if (rs.getRows().isEmpty()) {
            G.setDefaultCursor();
            return false;
        }
        else {
            setId(((Number)(rs.getRows().get(0).get("fun_id"))).intValue());
            G.setDefaultCursor();
            return true;
        }
    }
}
