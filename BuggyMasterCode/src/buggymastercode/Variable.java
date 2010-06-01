/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package buggymastercode;

/**
 *
 * @author jalvarez
 */
public class Variable {
    public String name = "";
    public boolean isString = false;
    public boolean isLong = false;
    public boolean isInt = false;
    public boolean isBoolean = false;

    public void setType(String dataType) {
        if (dataType.equals("String"))
            isString = true;
        else if (dataType.equals("long"))
            isLong = true;
        else if (dataType.equals("int"))
            isInt = true;
        else if (dataType.equals("boolean"))
            isBoolean = true;
    }
}
