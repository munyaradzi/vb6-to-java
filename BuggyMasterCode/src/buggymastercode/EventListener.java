/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package buggymastercode;

/**
 *
 * @author jalvarez
 */
public class EventListener {

    private String m_generator = "";
    private String m_adapter = "";
    private StringBuilder sourceCode = new StringBuilder();

    public String getGenerator() {
        return m_generator;
    }

    public void setGenerator(String value) {
        m_generator = value;
    }

    public void setAdapter(String value) {
        m_adapter = value;
    }

    public StringBuilder getSourceCode() {
        return sourceCode;
    }
}
