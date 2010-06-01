/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package buggymastercode;

/**
 *
 * @author jalvarez
 */
public class DBOracleBuggyMasterCode implements DBBuggyMasterCode {

    public boolean updateClass(DBConnection db, ClassObject classObj) {return false;}
    public boolean updateFunction(DBConnection db, FunctionObject functionObj) {return false;}
    public boolean updateVariable(DBConnection db, VariableObject variableObj) {return false;}

}
