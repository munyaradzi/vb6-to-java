/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package buggymastercode;

import java.awt.Cursor;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTable;

/**
 *
 * @author jalvarez
 */
public class G {

    static private final String C_SYMBOLS = " +-*/,;";
    static private final String C_SYMBOLS2 = " +-*/,;()[]{}";

    public static boolean isNumeric(Object value) {
        try {
            double d = Double.parseDouble(value.toString());
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    public static void showInfo(String msg) {
        JFrame mainFrame = BuggyMasterCodeApp.getApplication().getMainFrame();
        JOptionPane.showMessageDialog(mainFrame,msg);
    }

    public static boolean setRowSelectedById(JTable table, int id) {
        int rows = table.getModel().getRowCount();
        if (rows > 0) {
            int indexRow = -1;
            for (int i = 0; i < rows; i++) {
                if (id == Db.getId(table.getValueAt(i, 0))) {
                    indexRow = i;
                    break;
                }
            }
            if (indexRow >= 0) {
                return setRowSelected(table, indexRow);
            }
            else {
                return false;
            }
        } else {
            return false;
        }
    }

    public static boolean setRowSelected(JTable table, int indexRow) {
        if (indexRow >= 0 && indexRow < table.getModel().getRowCount()) {
            // The following row selection methods work only if these
            // properties are set this way table.setColumnSelectionAllowed(false);
            table.setRowSelectionAllowed(true);
            // Select a row - row indexRow
            table.setRowSelectionInterval(indexRow, indexRow);
            return true;
        } else {
            return false;
        }
    }

    public static void setHourglass() {
        setHourglass(BuggyMasterCodeApp.getApplication().getMainFrame());
    }
    public static void setHourglass(JFrame frame) {
        Cursor hourglassCursor = new Cursor(Cursor.WAIT_CURSOR);
        if (frame.getCursor() != hourglassCursor) {
            frame.setCursor(hourglassCursor);
        }
    }
    public static void setDefaultCursor() {
        setDefaultCursor(BuggyMasterCodeApp.getApplication().getMainFrame());
    }
    public static void setDefaultCursor(JFrame frame) {
        Cursor normalCursor = new Cursor(Cursor.DEFAULT_CURSOR);
        frame.setCursor(normalCursor);
    }

    public static String ltrim(String str)
    {
        //If the argument is null then return empty string
        if(str==null) return "";
        if(str.isEmpty()) return "";

        /* The charAt method returns the character at a particular position in a String.
         * We check to see if the character at position 0 (the leading character) is a space.
         * If it is, use substring to make a new String that starts after the space.
         */
        while(str.charAt(0) == ' ')
        {
            str = str.substring(1);
            if(str.isEmpty()) return "";
        }
        return str;
    }

    public static String rtrim(String str) {
        //If the argument is null then return empty string
        if(str==null) return "";
        if(str.isEmpty()) return "";

        /* The logic for Rtrim is, While the last character in the String is a space, remove it.
         * In the code, take the length of the string and use it to determine if the last character is a space.
         */
        int len = str.length();
        while(str.charAt(len-1) == ' ')
        {
            str = str.substring(0,len-1);
            len--;
        }
        return str;
    }

    public static String ltrimTab(String str)
    {
        //If the argument is null then return empty string
        if(str==null) return "";
        if(str.isEmpty()) return "";

        /* The charAt method returns the character at a particular position in a String.
         * We check to see if the character at position 0 (the leading character) is a space.
         * If it is, use substring to make a new String that starts after the space.
         */
        while(str.charAt(0) == ' ')
        {
            str = str.substring(1);
            if(str.isEmpty()) return "";
        }
        while(str.charAt(0) == '\t')
        {
            str = str.substring(1);
            if(str.isEmpty()) return "";
        }
        return str;
    }

    public static boolean beginLike(String source, String begin) {
        int len = begin.length();
        if (source.length() < len)
            return false;
        return (source.substring(0,len).equalsIgnoreCase(begin));
    }

    public static boolean endLike(String source, String end) {
        int len = end.length();
        if (source.length() < len)
            return false;
        return (source.substring(source.length()-len).equalsIgnoreCase(end));
    }

    public final static String rep(char c, int count) {
        char[] s = new char[count];
        for (int i = 0; i < count; i++) {
            s[i] = c;
        }
        return new String(s).intern();
    } // end rep

    public static boolean getToken(String vbpFile, String token, int line, ByRefString value) {
        int currLine=0;
        int lenToken = token.length();
        FileInputStream fstream = null;
        value.text = "";

        if (!token.substring(0,lenToken).equals("=")) {
            token += "=";
            lenToken++;
        }

        try {

            fstream = new FileInputStream(vbpFile);
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String strLine;
            while ((strLine = br.readLine()) != null) {
                if (strLine.substring(0,lenToken).equals(token)) {
                    currLine++;
                    if (currLine == line) {
                        value.text = strLine.substring(lenToken);
                        break;
                    }
                }
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(BuggyMasterCodeView.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        } catch (IOException ex) {
            Logger.getLogger(BuggyMasterCodeView.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        } finally {
            try {
                fstream.close();
                return true;
            } catch (IOException ex) {
                Logger.getLogger(BuggyMasterCodeView.class.getName()).log(Level.SEVERE, null, ex);
                return false;
            }
        }
    }

    public static boolean contains(String source, String toFind) {
        boolean literalFlag = false;
        String expression = "";
        int openParentheses = 0;

        for (int i = 0; i < source.length(); i++) {
            if (source.charAt(i) == '"') {
                literalFlag = !literalFlag;
            }
            if (!literalFlag) {

                if (openParentheses == 0) {
                    if (source.charAt(i) == '(') {
                        openParentheses++;
                    }
                    expression += String.valueOf(source.charAt(i));
                }
                else {
                    if (source.charAt(i) == '(') {
                        openParentheses++;
                    }
                    else if (source.charAt(i) == ')') {
                        openParentheses--;
                        if (openParentheses == 0) {
                            expression += String.valueOf(source.charAt(i));
                        }
                    }
                }
            }
        }
        return expression.contains(toFind);
    }

    public static String[] split(String strLine) {
        return split(strLine, C_SYMBOLS);
    }

    public static String[] split2(String strLine) {
        return split2(strLine, C_SYMBOLS2);
    }

    public static String[] split2(String strLine, String symbols) {
        boolean literalFlag = false;
        boolean numberFlag = false;
        String[] words = new String[500];
        String word = "";
        int j = 0;
        boolean wordEnded = false;

        for (int i = 0; i < strLine.length(); i++) {
            if (strLine.charAt(i) == '"') {
                literalFlag = !literalFlag;
            }
            if (!literalFlag) {

                if (strLine.charAt(i) == '#') {
                    numberFlag = !numberFlag;
                }

                if (!numberFlag) {

                    if (symbols.contains(String.valueOf(strLine.charAt(i)))) {
                        wordEnded = true;
                    }
                    if (wordEnded) {
                        j = addWord(word, strLine, words, i, j);
                        wordEnded = false;
                        if (!word.isEmpty()) {
                            word = "";
                        }
                    }
                    else {
                        word += String.valueOf(strLine.charAt(i));
                    }
                }
                else {
                    word += String.valueOf(strLine.charAt(i));
                }
            }
            else {
                word += String.valueOf(strLine.charAt(i));
            }
        }
        if (!word.isEmpty()) {
            words[j] = word;
            j++;
        }
        String[] rtn = new String[j];
        for (int i = 0; i < j; i++) {
            rtn[i] = words[i];
        }
        return rtn;
    }

    public static String[] split(String strLine, String symbols) {
        boolean literalFlag = false;
        boolean numberFlag = false;
        String[] words = new String[500];
        String word = "";
        int j = 0;
        int openParentheses = 0;
        boolean wordEnded = false;

        for (int i = 0; i < strLine.length(); i++) {
            if (strLine.charAt(i) == '"') {
                literalFlag = !literalFlag;
            }
            if (!literalFlag) {

                if (strLine.charAt(i) == '#') {
                    numberFlag = !numberFlag;
                }

                if (!numberFlag) {

                    if (openParentheses == 0) {
                        if (strLine.charAt(i) == '(') {
                            openParentheses++;
                            wordEnded = true;
                        }
                        else if (symbols.contains(String.valueOf(strLine.charAt(i)))) {
                            wordEnded = true;
                        }
                        if (wordEnded) {
                            j = addWord(word, strLine, words, i, j);
                            wordEnded = false;
                            if (!word.isEmpty()) {
                                word = "";
                            }
                        }
                        else {
                            word += String.valueOf(strLine.charAt(i));
                        }
                    }
                    else {
                        if (strLine.charAt(i) == '(') {
                            openParentheses++;
                            word += String.valueOf(strLine.charAt(i));
                        }
                        else if (strLine.charAt(i) == ')') {
                            openParentheses--;
                            if (openParentheses == 0) {
                                j = addWord(word, strLine, words, i, j);
                                wordEnded = false;
                                if (!word.isEmpty()) {
                                    word = "";
                                }
                            }
                            else {
                                word += String.valueOf(strLine.charAt(i));
                            }
                        }
                        else {
                            word += String.valueOf(strLine.charAt(i));
                        }
                    }
                }
                else {
                    word += String.valueOf(strLine.charAt(i));
                }
            }
            else {
                word += String.valueOf(strLine.charAt(i));
            }
        }
        if (!word.isEmpty()) {
            words[j] = word;
            j++;
        }
        String[] rtn = new String[j];
        for (int i = 0; i < j; i++) {
            rtn[i] = words[i];
        }
        return rtn;
    }

    private static int addWord(String word, String strLine, String[] words, int i, int j) {
        if (!word.isEmpty()) {
            /*if (j >= words.length) {
                int dummy = 0;
            }*/
            words[j] = word;
            word = "";
            j++;
        }
        /*if (j >= words.length) {
            int dummy = 0;
        }*/
        words[j] = String.valueOf(strLine.charAt(i));
        j++;
        return j;
    }
}
