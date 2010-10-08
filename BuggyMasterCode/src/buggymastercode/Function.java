/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package buggymastercode;

/**
 *
 * @author jalvarez
 */
public class Function {

    static private final String newline = "\n";

    private Variable m_returnType = new Variable();
    public String vbDeclaration = "";
    public String javaDeclaration = "";
    private boolean m_needReturnVariable = false;

    public String getJavaName() {
        return m_returnType.getJavaName();
    }

    public String getVbName() {
        return m_returnType.getVbName();
    }

    public Variable getReturnType() {
        return m_returnType;
    }

    public String getDeclarations() {
        return vbDeclaration + newline + newline + javaDeclaration;
    }

    public boolean getNeedReturnVariable() {
        return m_needReturnVariable;
    }

    public void setNeedReturnVariable(boolean value) {
        m_needReturnVariable = value;
    }

    @Override
    public String toString() {
        return getJavaName();
    }
}
