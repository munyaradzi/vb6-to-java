/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package buggymastercode;

import java.util.ArrayList;

/**
 *
 * @author jalvarez
 */
public class Type {

    public String javaName = "";
    public String vbName = "";
    public boolean isPublic = false;
    private StringBuilder m_javaCode = new StringBuilder();
    private StringBuilder m_vbCode = new StringBuilder();

    // the members variables of a type are like property get
    // so they are functions
    //
    private ArrayList<Function> m_memberVariables = new ArrayList<Function>();

    public StringBuilder getJavaCode() {
        return m_javaCode;
    }

    public StringBuilder getVbCode() {
        return m_vbCode;
    }

    public ArrayList<Function> getMembersVariables() {
        return m_memberVariables;
    }

    @Override
    public String toString() {
        return javaName;
    }

}
