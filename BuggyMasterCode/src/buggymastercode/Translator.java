/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package buggymastercode;

import java.util.ArrayList;
import java.util.Iterator;

/**
 *
 * @author jalvarez
 */
public class Translator {

    static private final String newline = "\n";
    static private final String C_NUMBERS = "-+0123456789";
    static private final String C_SEPARARTORS = "_._=_&&_||_+_-_*_/_==_!=_<_>_<=_>=_";
    static private final String C_SYMBOLS = " +-()*/,";
    static private final String C_RESERVED_WORDS =
        "_and_as_byval_byref_case_class_dim_elseif_else_end_each_for_friend_"
     + "_function_global_goto_if_in_is_next_not_of_or_on error_on resume_print_"
     + "_private_public_raise_select_sub_type_while_wend_char_date_double_integer_"
     + "_long_object_short_string_variant_#if_#end_exit_redim_on_";

    private boolean m_isVbSource = false;
    private boolean m_codeHasStarted = false;
    private boolean m_attributeBlockHasStarted = false;
    private boolean m_inFunction = false;
    private boolean m_inEnum = false;
    private boolean m_inWith = false;
    private boolean m_inType = false;
    private boolean m_withDeclaration = false;
    private boolean m_endWithDeclaration = false;
    private boolean m_emptyLine = false;
    private String m_returnValue = ""; // default value for function return

    private String[] m_iterators = {"","_i","_j","_k","_t","_w","_z"};
    private int m_iteratorIndex = 0;

    // member variables of the class which we are translating
    //
    private ArrayList<Variable> m_memberVariables = new ArrayList<Variable>();
    // parameters and local variables of the function which we are translating
    //
    private ArrayList<Variable> m_functionVariables = new ArrayList<Variable>();
    // public functions, subs and properties of the class which we are 
    // translating
    //
    private ArrayList<Function> m_publicFunctions = new ArrayList<Function>();
    // private functions, subs and properties of the class which we are
    // translating
    //
    private ArrayList<Function> m_privateFunctions = new ArrayList<Function>();
    // this is used to build the dictionary of public variables of every
    // class in this project. this collection is used to found identifiers
    // in the code which references to public member of objects of other
    // classes.
    // public variables are accessed by the dot operator and assigned using the
    // equal sign (=) eg: "m_objmember.publicVariable = 1;" by the other
    // hand public properties are translated as setters and getters and the
    // assignment doesn't use the equals sign but the setter method.
    //
    private ArrayList<Variable> m_publicVariables = new ArrayList<Variable>();
    // files (frm, bas, cls) in this vbp
    //
    private ArrayList<SourceFile> m_collFiles = new ArrayList<SourceFile>();
    private ArrayList<Variable> m_collWiths = new ArrayList<Variable>();
    private ArrayList<Type> m_types = new ArrayList<Type>();
    // classes in java (String, Date, etc.)
    //
    private ArrayList<SourceFile> m_collJavaClassess = new ArrayList<SourceFile>();

    // the current type which we are translaing
    //
    private String m_type = "";
    // the collection of every type public and private declared
    // in the class which we are translating
    //
    private ArrayList<String> m_collTypes = new ArrayList<String>();
    // the current enum which we are translating
    //
    private String m_enum = "";
    // the collection of every enum public and private declared
    // in the class which we are translating
    //
    private ArrayList<String> m_collEnums = new ArrayList<String>();

    // member variables which can raise events
    //
    private ArrayList<EventListener> m_eventListeners = new ArrayList<EventListener>();
    // the resulting interface declaration of add every public event declaration
    // in the class which we are translating
    //
    private String m_listenerInterface = "";
    // the resulting class declaration of add every public event declaration
    // in the class which we are translating with a null implementation
    // of every method
    //
    private String m_adapterClass = "";
    private boolean m_wasSingleLineIf = false;
    private String m_strBuffer = "";
    private int m_tabCount = 0;
    private String m_vbFunctionName = "";
    private String m_vbClassName = "";
    private String m_javaClassName = "";
    private boolean m_isFirstCase = false;
    private boolean m_previousWasReturn = false;
    private boolean m_addDateAuxFunction = false;
    private String m_packageName = "";
    private String[] m_references = null;

    private ClassObject m_classObject;
    private FunctionObject m_functionObject;
    private VariableObject m_variableObject;

    private TranslatorWorker m_caller = null;

    private ClassObject m_typeClassObject;

    public Translator() {
        m_collJavaClassess = new ArrayList<SourceFile>();
        SourceFile source = null;
        Function fun = null;

        // String
        //
        source = new SourceFile();
        source.setJavaName("String");
        source.setPublicFunctions(new ArrayList<Function>());

            // substring
            //
            fun = new Function();
            fun.getReturnType().setJavaName("substring");
            fun.getReturnType().setType("String");
            source.getPublicFunctions().add(fun);
            m_collJavaClassess.add(source);

            // toLowerCase
            //
            fun = new Function();
            fun.getReturnType().setJavaName("toLowerCase");
            fun.getReturnType().setType("String");
            source.getPublicFunctions().add(fun);
            m_collJavaClassess.add(source);

            // toUpperCase
            //
            fun = new Function();
            fun.getReturnType().setJavaName("toUpperCase");
            fun.getReturnType().setType("String");
            source.getPublicFunctions().add(fun);
            m_collJavaClassess.add(source);

            // trim
            //
            fun = new Function();
            fun.getReturnType().setJavaName("trim");
            fun.getReturnType().setType("String");
            source.getPublicFunctions().add(fun);
            m_collJavaClassess.add(source);
    }

    public void setCaller(TranslatorWorker caller) {
        m_caller = caller;
    }

    public void setPackage(String packageName) {
        m_packageName = packageName;
    }

    public void setReferences(String[] references) {
        m_references = references;
    }

    public void setSourceFiles(ArrayList<SourceFile> sourceFiles) {
        m_collFiles = sourceFiles;
    }

    public boolean isVbSource() {
        return m_isVbSource;
    }

    public String getVbClassName() {
        return m_vbClassName;
    }

    public String getJavaClassName() {
        return m_javaClassName;
    }

    public ArrayList<Function> getPublicFunctions() {
        return m_publicFunctions;
    }

    public ArrayList<Function> getPrivateFunctions() {
        return m_privateFunctions;
    }

    public ArrayList<Variable> getPublicVariables() {
        return m_publicVariables;
    }

    public boolean deletePackage(String packageName) {
        String sqlstmt = "delete from tvariable where cl_id in "
                            + "(select cl_id from tclass where cl_packagename = "
                            + Db.getString(packageName) + ")";
        if (Db.db.execute(sqlstmt)) {

            sqlstmt = "delete from tfunction where cl_id in "
                                + "(select cl_id from tclass where cl_packagename = "
                                + Db.getString(packageName) + ")";
            if (Db.db.execute(sqlstmt)) {

                sqlstmt = "delete from tclass where cl_packagename = "
                            + Db.getString(packageName);
                if (Db.db.execute(sqlstmt)) {
                    return true;
                }
                else {
                    return false;
                }
            }
            else {
                return false;
            }
        }
        else {
            return false;
        }
    }

    public void parse(String strLine) {
        if (m_isVbSource) {
            if (m_codeHasStarted) {
                parseLine(strLine);
            }
            else {
                if (strLine.contains("Attribute VB_Name = \"")) {
                    m_attributeBlockHasStarted = true;
                    m_vbClassName = strLine.substring(21, strLine.length()-1);
                    m_javaClassName = m_vbClassName;
                }
                else {
                    if (m_attributeBlockHasStarted) {
                        if (strLine.length() < 9) {
                            m_codeHasStarted = true;
                            parseLine(strLine);
                        }
                        else {
                            if (!strLine.substring(0,9).equals("Attribute")) {
                                m_codeHasStarted = true;
                                parseLine(strLine);
                            }
                        }
                    }
                    else {
                        if (strLine.length() >= 9) {
                            if (strLine.substring(0,9).equals("Attribute")) {
                                m_attributeBlockHasStarted = true;
                            }
                        }
                    }
                }
            }
        }
    }

    public String translate(String strLine) {
        String rtn = "";
        if (m_isVbSource) {
            if (m_codeHasStarted) {
                rtn = translateLine(strLine);
            }
            else {
                if (strLine.contains("Attribute VB_Name = \"")) {
                    m_attributeBlockHasStarted = true;
                    String className = strLine.substring(21, strLine.length()-1);
                    m_vbClassName = className;
                    m_javaClassName = m_vbClassName;
                    m_classObject.setPackageName(m_packageName);
                    m_classObject.setVbName(m_vbClassName);
                    m_classObject.setJavaName(m_javaClassName);
                    m_classObject.getClassIdFromClassName();
                    m_classObject.saveClass();
                    m_tabCount++;
                    rtn = "public class " + className + " {" + newline + newline;
                }
                else {
                    if (m_attributeBlockHasStarted) {
                        if (strLine.length() < 9) {
                            m_codeHasStarted = true;
                            rtn = translateLine(strLine);
                        }
                        else {
                            if (!strLine.substring(0,9).equals("Attribute")) {
                                m_codeHasStarted = true;
                                rtn = translateLine(strLine);
                            }
                            else
                                rtn = "";
                        }
                    }
                    else {
                        if (strLine.length() < 9) {
                            rtn = "";
                        }
                        else {
                            if (strLine.substring(0,9).equals("Attribute")) {
                                m_attributeBlockHasStarted = true;
                            }
                            rtn = "";
                        }
                    }
                }
            }
        }
        if (rtn.contains("return ")) {
            m_previousWasReturn = true;
        }
        else if (rtn.contains("return;")) {
            m_previousWasReturn = true;
        }
        else if (!rtn.trim().isEmpty()) {
            m_previousWasReturn = false;
        }
        if (m_emptyLine) {
            m_emptyLine = false;
            rtn = "";
        }
        return rtn;
    }

    public String getImportSection() {
        String rtn = "";

        if (m_addDateAuxFunction) {
            rtn = newline +
                    "import java.text.DateFormat;" + newline +
                    "import java.text.ParseException;" + newline +
                    "import java.text.SimpleDateFormat;" + newline +
                    "import java.util.Date;" + newline;
        }

        return rtn + newline;
    }

    public String getAuxFunctions() {
        String rtn = "";

        if (m_addDateAuxFunction) {
            rtn = newline +
                    "    private static Date getDateFromString(String date)" + newline +
                    "    {" + newline +
                    "        DateFormat df = new SimpleDateFormat(\"MM/dd/yyyy\");" + newline +
                    "        date = date.replace(\"#\",\"\");" + newline +
                    "        Date dateValue = null;" + newline +
                    "        try {" + newline +
                    "            dateValue = df.parse(date);" + newline +
                    "        } catch (ParseException ex) {/* it can not be possible*/}" + newline +
                    "        return dateValue;" + newline +
                    "    }" + newline;
        }

        return rtn;
    }

    private void parseLine(String strLine) {
        // two kind of sentences
            // In function
            // Declarations

        // functions
            // Function
            // Sub
            // Property

        // if the sentence is split in two or more lines
        // we need to join the lines before translate it
        //
        if (isSentenceComplete(strLine)) {

            strLine = m_strBuffer + G.ltrimTab(strLine);
            m_strBuffer = "";

            strLine = removeLineNumbers(strLine);

            if (isEmptyLine(strLine)) {
                return;
            }
            if (isVbSpecificCode(strLine)) {
                return;
            }
            if (isComment(strLine)) {
                return;
            }
            if (isDeclareApi(strLine)) {
                return;
            }
            else if (isBeginOfType(strLine)) {
                m_inType = true;
                return;
            }
            else if (isEndOfType(strLine)) {
                return;
            }
            if (m_inType) {
                return;
            }
            else if (isBeginOfEnum(strLine)) {
                m_inEnum = true;
                return;
            }
            else if (isEndOfEnum(strLine)) {
                return;
            }
            else if (m_inEnum) {
                return;
            }
            else if (isEndFunction(strLine)) {
                m_inFunction = false;
                return;
            }
            else if (m_inFunction) {
                return;
            }
            else {
                // first check for Function | Sub | Property
                if (isBeginOfFunction(strLine)) {
                    parseFunctionDeclaration(strLine);
                    return;
                }
                // declarations
                else {
                    return;
                }
            }
        }
        // split sentences
        else {
            m_strBuffer += G.rtrim(strLine.substring(0, strLine.length()-1)) + " " ;
            return;
        }
    }

    private String translateLine(String strLine) {
        // two kind of sentences
            // In function
            // Declarations

        // functions
            // Function
            // Sub
            // Property

        // if the sentence is split in two or more lines
        // we need to join the lines before translate it
        //
        if (isSentenceComplete(strLine)) {

            strLine = m_strBuffer + G.ltrimTab(strLine);
            m_strBuffer = "";

            strLine = removeLineNumbers(strLine);

            if (isEmptyLine(strLine)) {
                return strLine + newline;
            }
            if (isVbSpecificCode(strLine)) {
                return "//" + strLine + newline;
            }
            if (isComment(strLine)) {
                return getTabs() + commentLine(strLine);
            }
            if (isDeclareApi(strLine)) {
                return declareApiLine(strLine);
            }
            if (m_inType) {
                addToType(strLine);
                return "";
            }
            else if (isBeginOfType(strLine)) {
                addToType(strLine);
                return "//*TODO:** type is translated as a new class at the end of the file " + strLine + newline;
            }
            else if (isBeginOfEnum(strLine)) {
                addToEnum(strLine);
                return "//*TODO:** enum is translated as a new class at the end of the file " + strLine + newline;
            }
            else if (m_inEnum) {
                addToEnum(strLine);
                return "";
            }
            else if (m_inFunction) {
                checkEndBlock(strLine);
                String rtn = getTabs() + translateLineInFunction(strLine);
                checkBeginBlock(strLine);
                return rtn;
            }
            else {
                // first check for Function | Sub | Property
                if (isBeginOfFunction(strLine)) {
                    checkEndBlock(strLine);
                    String rtn = getTabs() + translateLineInFunction(strLine);
                    checkBeginBlock(strLine);
                    return rtn;
                }
                // declarations
                else {
                    checkEndBlock(strLine);
                    String rtn = getTabs() + translateLineInDeclaration(strLine);
                    checkBeginBlock(strLine);
                    return rtn;
                }
            }
        }
        // split sentences
        else {
            // TODO: delete after confirm the change is ok
            //
            //m_strBuffer += G.rtrim(strLine.substring(0, strLine.length()-1)) + " ";
            //
            m_strBuffer += strLine.substring(0, strLine.length()-1).trim() + " ";
            return "";
        }
    }

    private boolean isSentenceComplete(String strLine) {
        strLine = G.ltrimTab(strLine);
        if (strLine.isEmpty())
            return true;
        else if (getStartComment(strLine) >= 0)
            return true;
        else if (strLine.length() < 2)
            return true;
        else
            return !(strLine.substring(strLine.length()-2).equals(" _"));
    }

    private int getStartComment(String strLine) {
        boolean literalFlag = false;
        for (int i = 0; i < strLine.length(); i++) {
            if (strLine.charAt(i) == '"') {
                literalFlag = !literalFlag;
            }
            else if (strLine.charAt(i) == '\'') {
                if (!literalFlag) {
                    return i;
                }
            }
        }
        return -1;
    }

    private boolean isEmptyLine(String strLine) {
        strLine = G.ltrimTab(strLine);
        return strLine.isEmpty();
    }

    private boolean isVbSpecificCode(String strLine) {
        strLine = G.ltrimTab(strLine).toLowerCase();
        if (strLine.isEmpty())
            return false;
        if (strLine.equals("option explicit"))
            return true;
        else
            return false;
    }

    private boolean isComment(String strLine) {
        strLine = G.ltrimTab(strLine);
        if (strLine.isEmpty())
            return false;
        else
            return strLine.charAt(0) == '\'';
    }

    private String commentLine(String strLine) {
        return G.ltrimTab(strLine.replaceFirst("'", "//")) + newline;
    }

    private boolean isDeclareApi(String strLine) {
        strLine = G.ltrimTab(strLine);
        if (strLine.isEmpty()) {
            return false;
        }
        else {
            if (strLine.length() >= 15) {
                if (strLine.substring(0, 15).equalsIgnoreCase("public declare ")) {
                    return true;
                }
                else if (strLine.length() >= 16) {
                    return strLine.substring(0, 16).equalsIgnoreCase("private declare ");
                }
                else {
                    return false;
                }
            }
            else {
                return false;
            }
        }
    }

    private String declareApiLine(String strLine) {
        return "*TODO: API " + strLine + newline;
    }

    private boolean isBeginOfEnum(String strLine) {
        strLine = G.ltrim(strLine);
        strLine = removeExtraSpaces(strLine);
        if (strLine.length() > 5) {
            if (strLine.substring(0,5).toLowerCase().equals("enum ")) {
                m_inEnum = true;
                return true;
            }
        }
        if (strLine.length() > 12) {
            if (strLine.substring(0,12).toLowerCase().equals("public enum ")) {
                m_inEnum = true;
                return true;
            }
        }
        if (strLine.length() > 13) {
            if (strLine.substring(0,13).toLowerCase().equals("private enum ")) {
                m_inEnum = true;
                return true;
            }
        }
        return false;
    }

    private boolean isEndOfEnum(String strLine) {
        strLine = G.ltrim(strLine);

        if (strLine.trim().length() == 8) {
            if (strLine.substring(0,8).toLowerCase().equals("end enum")) {
                m_inEnum = false;
                return true;
            }
        }
        return false;
    }

    private boolean isBeginOfType(String strLine) {
        strLine = G.ltrim(strLine);
        strLine = removeExtraSpaces(strLine);
        if (strLine.length() > 5) {
            if (strLine.substring(0,5).toLowerCase().equals("type ")) {
                m_inType = true;
                return true;
            }
        }
        if (strLine.length() > 12) {
            if (strLine.substring(0,12).toLowerCase().equals("public type ")) {
                m_inType = true;
                return true;
            }
        }
        if (strLine.length() > 13) {
            if (strLine.substring(0,13).toLowerCase().equals("private type ")) {
                m_inType = true;
                return true;
            }
        }
        return false;
    }

    private boolean isEndOfType(String strLine) {
        strLine = G.ltrim(strLine);

        if (strLine.trim().length() == 8) {
            if (strLine.substring(0,8).toLowerCase().equals("end type")) {
                m_inType = false;
                return true;
            }
        }
        return false;
    }

    private boolean isBeginOfFunction(String strLine) {
        // functions
            // Function
            // Sub
            // Property
        strLine = strLine.toLowerCase();
        strLine = strLine.replaceAll("public", "");
        strLine = strLine.replaceAll("private", "");
        strLine = strLine.replaceAll("friend", "");
        strLine = G.ltrimTab(strLine);
        if (strLine.length() > 10) {
            if (strLine.substring(0,9).toLowerCase().equals("function ")) {
                m_inFunction = true;
                return true;
            }
        }
        if (strLine.length() > 5) {
            if (strLine.substring(0,4).toLowerCase().equals("sub ")) {
                m_inFunction = true;
                return true;
            }
        }
        if (strLine.length() > 10) {
            if (strLine.substring(0,9).toLowerCase().equals("property ")) {
                m_inFunction = true;
                return true;
            }
        }
        return false;
    }

    // posible lines
        // declarations
        // block: If , While, For, Do, Case
        // asignment
        // calls
        // others: comments, blank, visual basic expecific liek #If

    private String translateLineInDeclaration(String strLine) {
        // declaration expecific stuff
        //
        return translateCode(strLine, true);
    }
    private String translateLineInFunction(String strLine) {
        // function expecific stuff
        //
        strLine = translateCode(strLine, false);

        // translate inner with block
        //
        if (m_inWith) {
            if (!m_withDeclaration && !m_endWithDeclaration) {
                boolean evalWith = false;
                if (strLine.contains(" ."))
                    evalWith = true;
                else if (strLine.contains("(."))
                    evalWith = true;
                else if (strLine.contains("\t."))
                    evalWith = true;
                else if (strLine.contains("!."))
                    evalWith = true;
                if (evalWith) {
                    String withVariable = m_collWiths.get(m_collWiths.size()-1).getJavaName();
                    String workLine = "";
                    boolean literalFlag = false;
                    for (int i = 0; i < strLine.length(); i++) {
                        if (strLine.charAt(i) == '"') {
                            literalFlag = !literalFlag;
                        }
                        else if (!literalFlag) {
                            if (strLine.charAt(i) == '.') {
                                if (i > 0) {
                                    if (strLine.charAt(i - 1) == ' ') {
                                        workLine += withVariable;
                                    }
                                    else if (strLine.charAt(i - 1) == '(') {
                                        workLine += withVariable;
                                    }
                                    else if (strLine.charAt(i - 1) == '\t') {
                                        workLine += withVariable;
                                    }
                                    else if (strLine.charAt(i - 1) == '!') {
                                        workLine += withVariable;
                                    }
                                }
                                else {
                                    workLine += withVariable;
                                }
                            }
                        }
                        workLine += strLine.charAt(i);
                    }
                    strLine = workLine;
                }
            }
        }
        return strLine;
    }

    private String translateCode(String strLine, boolean inDeclaration) {
        // first we extract comments
        // so the code only works over executable code
        //
        int startComment = getStartComment(strLine);
        String workLine = strLine;
        String comments = "";
        if (startComment >= 0) {
            comments =  "//" + workLine.substring(startComment);
            workLine = workLine.substring(0, startComment-1);
        }

        String rtn = translateCodeAux(workLine, inDeclaration);
        rtn = translateDateConstant(rtn);
        rtn = translateUbound(rtn);
        rtn = translateIsNull(rtn);

        if (!comments.isEmpty())
            rtn = comments + newline + getTabs() + rtn;

        //rtn = translateComments(rtn);
        return rtn;
    }

    private String translateCodeAux(String strLine, boolean inDeclaration) {
        // get out spaces even tabs
        //
        String workLine = G.ltrimTab(strLine).toLowerCase();
        // dim
        if (workLine.length() > 4) {
            if (workLine.substring(0,4).equals("dim ")) {
                return translateDim(strLine);
            }
        }
        // in declaration
            // private and public can be modifier of member variables
            //
        if (inDeclaration) {
            if (workLine.length() > 8) {
                if (workLine.substring(0,8).equals("private ")) {
                    if (workLine.contains(" const ")) {
                        return translatePrivateConstMember(strLine);                        
                    }
                    else {
                        return translatePrivateMember(strLine);
                    }
                }
            }
            if (workLine.length() > 7) {
                if (workLine.substring(0,7).equals("public ")) {
                    if (workLine.contains(" const ")) {
                        return translatePublicConstMember(strLine);
                    }
                    else {
                        return translatePublicMember(strLine);
                    }
                }
            }
        }
        // in function
            // private and public only can be modifier of functions
            //
        else {
            // a function declaration is like this
                // Public Function ShowPrintDialog(ByVal
            if (isFunctionDeclaration(workLine)) {
                strLine = translateFunctionDeclaration(strLine);
                checkEventHandler(strLine);
                return strLine;
            }
            else {
                if (isEndFunction(workLine)) {
                    return "}" + newline;
                }
                // function's body
                //
                else {
                    // types of sentences
                        // conditional block
                            // if, select case, elseif, else
                    if (isIfSentence(workLine))
                        return translateIfSentence(strLine);
                    if (isElseIfSentence(workLine))
                        return translateElseIfSentence(strLine);
                    else if (isElseSentence(workLine))
                        return translateElseSentence(strLine);
                    else if (isEndIfSentence(workLine))
                        return translateEndIfSentence(strLine);
                    else if (isSelectCaseSentence(workLine))
                        return translateSelectCaseSentence(strLine);
                    else if (isCaseSentence(workLine))
                        return translateCaseSentence(strLine);
                    else if (isEndSelectSentence(workLine))
                        return translateEndSelectSentence(strLine);
                    else if (isExitFunctionSentence(workLine))
                        return translateExitFunctionSentence(strLine);
                    else if (isWhileSentence(workLine))
                        return translateWhileSentence(strLine);
                    else if (isWendSentence(workLine))
                        return translateWendSentence(strLine);
                    else if (isForSentence(workLine))
                        return translateForSentence(strLine);
                    else if (isNextSentence(workLine))
                        return translateNextSentence(strLine);
                    else
                        return translateSentenceWithNewLine(strLine);
                        // loop block
                            // for, while, do, loop

                        // asignment sentence
                            // set, =

                        // call sentence
                }
            }
        }
        return "*" + strLine + newline;
    }

    private String translateDateConstant(String strLine) {
        String rtn = "";
        String[] words = G.split(strLine);
        for (int i = 0; i < words.length; i++) {
            if (words[i].length() >= 8) {
                if (words[i].charAt(0) == '#') {
                    if (words[i].charAt(words[i].length() - 1) == '#') {
                        words[i] = "getDateFromString("
                                    + words[i].substring(1, words[i].length() - 1)
                                    + ")";
                        m_addDateAuxFunction = true;
                    }
                }
            }
            rtn += words[i];
        }
        return rtn;
    }

    private String translateUbound(String strLine) {
        boolean openParentheses = false;
        boolean uboundFound = false;
        int iOpenParentheses = 0;
        String arrayExpression = "";
        String rtn = "";
        String[] words = G.split(strLine);
        for (int i = 0; i < words.length; i++) {
            if (uboundFound) {
                if (words[i].equals("(")) {
                    iOpenParentheses++;
                    if (iOpenParentheses > 1) {
                        arrayExpression += words[i];
                    }
                }
                else if (words[i].equals(")")) {
                    iOpenParentheses--;
                    if (iOpenParentheses == 0) {
                        if (arrayExpression.contains(" ")) {
                            rtn += "(" + arrayExpression + ").length";
                        }
                        else {
                            rtn += arrayExpression + ".length";
                        }
                        uboundFound = false;
                    }
                    else {
                        arrayExpression += words[i];
                    }

                }
                else if (!words[i].equalsIgnoreCase("Ubound")) {
                    arrayExpression += words[i];
                }
            }
            else {
                if (words[i].equals("(")) {
                    openParentheses = true;
                }
                else {
                    if (openParentheses) {
                        openParentheses = false;
                        words[i] = translateUbound(words[i]);
                    }
                    else {
                        if (words[i].length() == 6) {
                            if (words[i].equalsIgnoreCase("Ubound")) {
                                uboundFound = true;
                            }
                        }
                    }
                }
                if (!uboundFound) {
                    rtn += words[i];
                }
            }
        }
        return rtn;
    }

    private String translateIsNull(String strLine) {
        boolean openParentheses = false;
        boolean isNullFound = false;
        int iOpenParentheses = 0;
        String nullExpression = "";
        String rtn = "";
        String[] words = G.split(strLine);
        for (int i = 0; i < words.length; i++) {
            if (isNullFound) {
                if (words[i].equals("(")) {
                    iOpenParentheses++;
                    if (iOpenParentheses > 1) {
                        nullExpression += words[i];
                    }
                }
                else if (words[i].equals(")")) {
                    iOpenParentheses--;
                    if (iOpenParentheses == 0) {
                        if (nullExpression.contains(" ")) {
                            rtn += "(" + nullExpression + ") == null";
                        }
                        else {
                            rtn += nullExpression + " == null";
                        }
                        isNullFound = false;
                    }
                    else {
                        nullExpression += words[i];
                    }

                }
                else if (!words[i].equalsIgnoreCase("IsNull")) {
                    nullExpression += words[i];
                }
            }
            else {
                if (words[i].equals("(")) {
                    openParentheses = true;
                }
                else {
                    if (openParentheses) {
                        openParentheses = false;
                        words[i] = translateIsNull(words[i]);
                    }
                    else {
                        if (words[i].length() == 6) {
                            if (words[i].equalsIgnoreCase("IsNull")) {
                                isNullFound = true;
                            }
                        }
                    }
                }
                if (!isNullFound) {
                    rtn += words[i];
                }
            }
        }
        return rtn;
    }

    private void parseFunctionDeclaration(String strLine) {
        // get out spaces even tabs
        //
        String workLine = G.ltrimTab(strLine).toLowerCase();
        // dim
        if (workLine.length() > 4) {
            if (workLine.substring(0,4).equals("dim ")) {
                return;
            }
        }
        // in function
            // private and public only can be modifier of functions
            //

        // a function declaration is like this
            // Public Function ShowPrintDialog(ByVal
        if (isFunctionDeclaration(workLine)) {
            String functionDeclaration = translateFunctionDeclaration(strLine);
            String[] words = G.splitSpace(functionDeclaration);//functionDeclaration.split("\\s+");
            if (words.length >= 3) {
                Function function = new Function();
                function.vbDeclaration = strLine;
                function.javaDeclaration = functionDeclaration;
                if (words[2].contains("(")) {
                    int i = words[2].indexOf("(");
                    function.getReturnType().setJavaName(words[2].substring(0,i));
                }
                else
                    function.getReturnType().setJavaName(words[2]);
                function.getReturnType().setVbName(m_vbFunctionName);
                function.getReturnType().setType(words[1]);
                if (words[0].equals("private")) {
                    m_privateFunctions.add(function);
                }
                else {
                    m_publicFunctions.add(function);
                }
            }
        }
    }

    private boolean isCaseSentence(String strLine) {
        if (G.beginLike(strLine, "Case ")) {
            return true;
        }
        else
            return false;
    }

    private boolean isSelectCaseSentence(String strLine) {
        if (G.beginLike(strLine, "Select Case ")) {
            return true;
        }
        else
            return false;
    }

    private boolean isIfSentence(String strLine) {
        if (G.beginLike(strLine, "If ")) {
            return true;
        }
        else
            return false;
    }

    private boolean isWhileSentence(String strLine) {
        if (G.beginLike(strLine, "While ")) {
            return true;
        }
        else
            return false;
    }

    private boolean isForSentence(String strLine) {
        if (G.beginLike(strLine, "For ")) {
            return true;
        }
        else
            return false;
    }

    private boolean isElseIfSentence(String strLine) {
        if (G.beginLike(strLine, "ElseIf ")) {
            return true;
        }
        else
            return false;
    }

    private boolean isElseSentence(String strLine) {
        if (G.beginLike(strLine, "Else ")) {
            return true;
        }
        else {
            int startComment = getStartComment(strLine);
            if (startComment >= 0) {
                strLine = strLine.substring(0, startComment-1);
            }
            strLine = G.ltrimTab(strLine);
            if (strLine.equalsIgnoreCase("Else")) {
                return true;
            }
            else {
                return false;
            }
        }
    }

    private boolean isEndSelectSentence(String strLine) {
        if (G.beginLike(strLine, "End Select ")) {
            return true;
        }
        else {
            int startComment = getStartComment(strLine);
            if (startComment >= 0) {
                strLine = strLine.substring(0, startComment-1);
            }
            strLine = G.ltrimTab(strLine);
            if (strLine.equalsIgnoreCase("End Select")) {
                return true;
            }
            else {
                return false;
            }
        }
    }

    private boolean isExitFunctionSentence(String strLine) {
        if (G.beginLike(strLine, "Exit Function ")) {
            return true;
        }
        else {
            int startComment = getStartComment(strLine);
            if (startComment >= 0) {
                strLine = strLine.substring(0, startComment-1);
            }
            strLine = G.ltrimTab(strLine);
            if (strLine.equalsIgnoreCase("Exit Function")) {
                return true;
            }
            else {
                return false;
            }
        }
    }

    private boolean isWendSentence(String strLine) {
        if (G.beginLike(strLine, "Wend ")) {
            return true;
        }
        else {
            int startComment = getStartComment(strLine);
            if (startComment >= 0) {
                strLine = strLine.substring(0, startComment-1);
            }
            strLine = G.ltrimTab(strLine);
            if (strLine.equalsIgnoreCase("Wend")) {
                return true;
            }
            else {
                return false;
            }
        }
    }

    private boolean isNextSentence(String strLine) {
        if (G.beginLike(strLine, "Next ")) {
            return true;
        }
        else {
            int startComment = getStartComment(strLine);
            if (startComment >= 0) {
                strLine = strLine.substring(0, startComment-1);
            }
            strLine = G.ltrimTab(strLine);
            if (strLine.equalsIgnoreCase("Next")) {
                return true;
            }
            else {
                return false;
            }
        }
    }

    private boolean isEndIfSentence(String strLine) {
        if (G.beginLike(strLine, "End If ")) {
            return true;
        }
        else {
            int startComment = getStartComment(strLine);
            if (startComment >= 0) {
                strLine = strLine.substring(0, startComment-1);
            }
            strLine = G.ltrimTab(strLine);
            if (strLine.equalsIgnoreCase("End If")) {
                return true;
            }
            else {
                return false;
            }
        }
    }

    private String translateComments(String strLine) {
        // We only translate ' in // if the line doesn't contain a // yet
        // because if the line does, it means that the comments
        // has already been translated
        //
        if (!strLine.contains("//")) {
            int startComment = getStartComment(strLine);
            if (startComment >= 0) {
                strLine = strLine.substring(0, startComment-1)
                            + " //"
                            + strLine.substring(startComment);
            }
        }
        return strLine;
    }

    private String translateCaseSentence(String strLine) {
        String switchStatetment = "";
        boolean identifierHasStarted = false;
        boolean parenthesesClosed = false;
        String[] words = G.split(strLine);

        if (m_isFirstCase) {
            m_isFirstCase = false;
            switchStatetment = "case ";
        }
        else {
            switchStatetment = "    break;" + newline + getTabs() + "case ";
        }

        for (int i = 0; i < words.length; i++) {
            if (identifierHasStarted) {
                if (G.beginLike(words[i], "'")) {
                    switchStatetment += ": //";
                    parenthesesClosed = true;
                }
                if (!parenthesesClosed) {
                    if (words[i].equals(",")) {
                        switchStatetment += ":" + newline + getTabs() + "case ";
                    }
                    else {
                        switchStatetment += words[i];
                    }
                }
                else {
                    switchStatetment += words[i];
                }
            }
            else {
                if (words[i].toLowerCase().equals("case"))
                    identifierHasStarted = true;
            }
        }
        if (!parenthesesClosed)
            switchStatetment += ":";
        return switchStatetment + newline;
    }

    private String translateSelectCaseSentence(String strLine) {
        String switchStatetment = "";
        boolean identifierHasStarted = false;
        boolean parenthesesClosed = false;
        String[] words = G.split(strLine);

        m_isFirstCase = true;

        for (int i = 0; i < words.length; i++) {
            if (identifierHasStarted) {
                if (G.beginLike(words[i], "'")) {
                    switchStatetment += ") { //";
                    parenthesesClosed = true;
                }
                switchStatetment += words[i];
            }
            else {
                if (words[i].toLowerCase().equals("case"))
                    identifierHasStarted = true;
            }
        }
        if (!parenthesesClosed)
            switchStatetment += ") {";
        return "switch (" + switchStatetment.trim() + newline;
    }

    private String translateIfSentence(String strLine) {
        // the if block can contain an or more call sentence
        // and one or more logic operators and unary operators
        // binary operators: and, or
        // unary operator: not
        //
        boolean literalFlag = false;
        boolean thenFound = false;
        boolean previousWasNot = false;
        boolean previousWasParentheses = false;
        boolean isFirstWord = true;
        String javaSentenceIf = "";
        String javaSentenceBlock = "";
        String comments = "";

        int startComment = getStartComment(strLine);
        if (startComment >= 0) {
            comments =  "//" + strLine.substring(startComment);
            strLine = strLine.substring(0, startComment-1);
        }

        String[] words = G.splitSpace(strLine);//strLine.split("\\s+");

        // we start in 1 because word[0] is "If"
        //
        for (int i = 1; i < words.length; i++) {
            // typical sentence:
                // " if x then " -> 3 words
                // " if x and z then " -> 5 words
                // " if x and callFunction() then " -> 5 words
                // " if ((x or z) and y) or callFunction(param1, param2, param3)) then " -> too many words :)
                //
            // rules
                // 1- we have to add parentheses
                // 2- we have to respect parentheses
                // 3- we have to detect function calls
                // 4- we have to translate "or", "and", and "not"

            for (int j = 0; j < words[i].length(); j++) {
                if (words[i].charAt(j) == '"') {
                    literalFlag = !literalFlag;
                }
            }

            if (literalFlag) {
                if (thenFound) {
                    javaSentenceBlock += " " + words[i];
                }
                else {
                    javaSentenceIf += " " + words[i];
                }
            }
            else {
                if (thenFound) {
                    javaSentenceBlock += " " + words[i];
                }
                else {
                    if (words[i].equalsIgnoreCase("then")) {
                        thenFound = true;
                    }
                    else if (words[i].equalsIgnoreCase("and")) {
                        javaSentenceIf += " &&";
                    }
                    else if (words[i].equalsIgnoreCase(")and")) {
                        javaSentenceIf += ") &&";
                    }
                    else if (words[i].equalsIgnoreCase("and(")) {
                        javaSentenceIf += " && (";
                    }
                    else if (words[i].equalsIgnoreCase("or")) {
                        javaSentenceIf += " ||";
                    }
                    else if (words[i].equalsIgnoreCase(")or")) {
                        javaSentenceIf += ") ||";
                    }
                    else if (words[i].equalsIgnoreCase("or(")) {
                        javaSentenceIf += " || (";
                    }
                    else if (words[i].equalsIgnoreCase("<>")) {
                        javaSentenceIf += " !=";
                    }
                    else if (words[i].equalsIgnoreCase("not")) {
                        javaSentenceIf += " !";
                    }
                    else if (words[i].equals("(")) {
                        if (previousWasNot)
                            javaSentenceIf += "(";
                        else
                            javaSentenceIf += " (";
                    }
                    else if (words[i].equals(")")) {
                        javaSentenceIf += ")";
                    }
                    else if (words[i].equalsIgnoreCase("=")) {
                        javaSentenceIf += " ==";
                    }
                    else {
                        if (isFirstWord) {
                            javaSentenceIf += words[i];
                            isFirstWord = false;
                        }
                        else if (previousWasNot || previousWasParentheses) {
                            javaSentenceIf += words[i];
                        }
                        else {
                            javaSentenceIf += " " + words[i];
                        }
                    }

                    // flags
                    //
                    if (words[i].equalsIgnoreCase("not")) {
                        previousWasNot = true;
                    }
                    else {
                        previousWasNot = false;
                    }

                    if (words[i].charAt(words[i].length()-1) == '(') {
                        previousWasParentheses = true;
                    }
                    else {
                        previousWasParentheses = false;
                    }
                }
            }
        }
        if (javaSentenceBlock.isEmpty()) {
            return "if (" + translateSentence(javaSentenceIf) + ") {"
                    + comments + newline;
        }
        else {
            return "if (" + translateSentence(javaSentenceIf) + ") { "
                    + translateSentenceWithColon(G.ltrimTab(javaSentenceBlock))
                    + " }" + comments + newline;
        }
    }

    private String translateElseIfSentence(String strLine) {
        if (m_wasSingleLineIf) {
            return "else " + translateIfSentence(strLine);
        }
        else {
            return "} "
                    + newline
                    + getTabs()
                    + "else "
                    + translateIfSentence(strLine);
        }
    }

    private String translateElseSentence(String strLine) {
        String javaSentenceBlock = "";
        String comments = "";
        int startComment = getStartComment(strLine);
        if (startComment >= 0) {
            comments =  " //" + strLine.substring(startComment);
            strLine = strLine.substring(0, startComment-1);
        }
        strLine = strLine.trim();
        if (!strLine.equalsIgnoreCase("Else")) {
            javaSentenceBlock = " "
                                + translateSentenceWithColon(G.ltrimTab(strLine.substring(4)))
                                + " }";
        }
        if (m_wasSingleLineIf) {
            return "else {"
                    + javaSentenceBlock
                    + comments
                    + newline;
        }
        else {
            return "} "
                    + newline
                    + getTabs()
                    + "else {"
                    + javaSentenceBlock
                    + comments
                    + newline;
        }
    }

    private String translateEndIfSentence(String strLine) {
        int startComment = getStartComment(strLine);
        if (startComment >= 0) {
            String comments = "";
            comments =  "//" + strLine.substring(startComment);
            return "} " + comments + newline;
        }
        else {
            return "}" + newline;
        }
    }

    private String translateEndSelectSentence(String strLine) {
        int startComment = getStartComment(strLine);
        if (startComment >= 0) {
            String comments = "";
            comments =  "//" + strLine.substring(startComment);
            return "        break;" + newline + getTabs() + "} " + comments + newline;
        }
        else {
            return "        break;" + newline + getTabs() + "}" + newline;
        }
    }

    private String translateExitFunctionSentence(String strLine) {
        int startComment = getStartComment(strLine);
        if (startComment >= 0) {
            String comments = "";
            comments =  "//" + strLine.substring(startComment);
            if (m_previousWasReturn)
                return comments + newline;
            else
                return "return null;" + comments + newline;
        }
        else {
            if (m_previousWasReturn) {
                m_emptyLine = true;
                return "";
            }
            else {
                return "return null;" + newline;
            }
        }
    }

    private String translateWhileSentence(String strLine) {
        // the while block can contain an or more call sentence
        // and one or more logic operators and unary operators
        // binary operators: and, or
        // unary operator: not
        //
        boolean literalFlag = false;
        boolean previousWasNot = false;
        boolean previousWasParentheses = false;
        boolean isFirstWord = true;
        String javaSentenceWhile = "";
        String comments = "";

        int startComment = getStartComment(strLine);
        if (startComment >= 0) {
            comments =  "//" + strLine.substring(startComment);
            strLine = strLine.substring(0, startComment-1);
        }

        String[] words = G.splitSpace(strLine);//strLine.split("\\s+");

        // we start in 1 because word[0] is "If"
        //
        for (int i = 1; i < words.length; i++) {
            // typical sentence:
                // " if x then " -> 3 words
                // " if x and z then " -> 5 words
                // " if x and callFunction() then " -> 5 words
                // " if ((x or z) and y) or callFunction(param1, param2, param3)) then " -> too many words :)
                //
            // rules
                // 1- we have to add parentheses
                // 2- we have to respect parentheses
                // 3- we have to detect function calls
                // 4- we have to translate "or", "and", and "not"

            for (int j = 0; j < words[i].length(); j++) {
                if (words[i].charAt(j) == '"') {
                    literalFlag = !literalFlag;
                }
            }

            if (literalFlag) {
                javaSentenceWhile += " " + words[i];
            }
            else {
                if (words[i].equalsIgnoreCase("and")) {
                    javaSentenceWhile += " &&";
                }
                else if (words[i].equalsIgnoreCase(")and")) {
                    javaSentenceWhile += ") &&";
                }
                else if (words[i].equalsIgnoreCase("and(")) {
                    javaSentenceWhile += " && (";
                }
                else if (words[i].equalsIgnoreCase("or")) {
                    javaSentenceWhile += " ||";
                }
                else if (words[i].equalsIgnoreCase(")or")) {
                    javaSentenceWhile += ") ||";
                }
                else if (words[i].equalsIgnoreCase("or(")) {
                    javaSentenceWhile += " || (";
                }
                else if (words[i].equalsIgnoreCase("<>")) {
                    javaSentenceWhile += " !=";
                }
                else if (words[i].equalsIgnoreCase("not")) {
                    javaSentenceWhile += " !";
                }
                else if (words[i].equals("(")) {
                    if (previousWasNot)
                        javaSentenceWhile += "(";
                    else
                        javaSentenceWhile += " (";
                }
                else if (words[i].equals(")")) {
                    javaSentenceWhile += ")";
                }
                else if (words[i].equalsIgnoreCase("=")) {
                    javaSentenceWhile += " ==";
                }
                else {
                    if (isFirstWord) {
                        javaSentenceWhile += words[i];
                        isFirstWord = false;
                    }
                    else if (previousWasNot || previousWasParentheses) {
                        javaSentenceWhile += words[i];
                    }
                    else {
                        javaSentenceWhile += " " + words[i];
                    }
                }

                // flags
                //
                if (words[i].equalsIgnoreCase("not")) {
                    previousWasNot = true;
                }
                else {
                    previousWasNot = false;
                }

                if (words[i].charAt(words[i].length()-1) == '(') {
                    previousWasParentheses = true;
                }
                else {
                    previousWasParentheses = false;
                }
            }
        }
        return "while (" + translateSentence(javaSentenceWhile) + ") {"
                + comments + newline;
    }

    private String translateForSentence(String strLine) {

        m_iteratorIndex++;

        // the for block can have three forms:
        //   for each var in collection
        //   for var = value_x to value_y
        //   for var = value_x to value_y step step_value
        //
        boolean literalFlag = false;
        boolean eachFound = false;
        boolean toFound = false;
        boolean inFound = false;
        boolean equalsFound = false;
        boolean stepFound = false;
        String iterator = "";
        String endValue = "";
        String startValue = "";
        String increment = "";
        String step = "";
        String collection = "";
        String comments = "";

        int startComment = getStartComment(strLine);
        if (startComment >= 0) {
            comments =  "//" + strLine.substring(startComment);
            strLine = strLine.substring(0, startComment-1);
        }

        String[] words = G.splitSpace(strLine);//strLine.split("\\s+");

        // we start in 1 because word[0] is "For"
        //
        for (int i = 1; i < words.length; i++) {
            // typical sentence:
                //   for each var in collection
                //   for var = value_x to value_y
                //   for var = value_x to value_y step step_value
                //
            // rules
                // 1- we have to add parentheses
                // 2- we have to respect parentheses
                // 3- we have to detect function calls

            for (int j = 0; j < words[i].length(); j++) {
                if (words[i].charAt(j) == '"') {
                    literalFlag = !literalFlag;
                }
            }

            if (literalFlag) {
                if (eachFound) {
                    if (inFound) {
                        collection += " " + words[i];
                    }
                    else {
                        iterator += " " + words[i];
                    }
                }
                else if (equalsFound) {
                    if (stepFound) {
                        step += " " + words[i];
                    }
                    else if (toFound) {
                        endValue += " " + words[i];
                    }
                    else {
                        startValue += " " + words[i];
                    }
                }
                else {
                    iterator += " " + words[i];
                }
            }
            else {
                if (eachFound) {
                    if (inFound) {
                        collection += " " + words[i];
                    }
                    else if (words[i].equalsIgnoreCase("in")) {
                        inFound = true;
                    }
                    else {
                        iterator += " " + words[i];
                    }
                }
                else if (equalsFound) {
                    if (stepFound) {
                        step += " " + words[i];
                    }
                    else if (words[i].equalsIgnoreCase("step")) {
                        stepFound = true;
                    }
                    else if (toFound) {
                        endValue += " " + words[i];
                    }
                    else if (words[i].equalsIgnoreCase("to")) {
                        toFound = true;
                    }
                    else {
                        startValue += " " + words[i];
                    }
                }
                else {
                    if (words[i].equalsIgnoreCase("each")) {
                        eachFound = true;
                    }
                    else if (words[i].equalsIgnoreCase("=")) {
                        equalsFound = true;
                    }
                    else if (words[i].equals("(")) {
                        iterator += "(";
                    }
                    else if (words[i].equals(")")) {
                        iterator += ")";
                    }
                    else {
                        iterator += " " + words[i];
                    }
                }
            }
        }
        if (eachFound) {
            collection = collection.trim();
            iterator = iterator.trim();
            return "for (int " + m_iterators[m_iteratorIndex] + " = 0;"
                            + " " + m_iterators[m_iteratorIndex] + " < "
                            + translateSentence(collection) + ".size();"
                            + " " + m_iterators[m_iteratorIndex] + "++) {"
                            + comments + newline
                            + getTabs() + "    "
                            + iterator + " = " + collection 
                            + ".getItem(" + m_iterators[m_iteratorIndex] + ");" + newline;
        }
        else {

            if (step.replace(" ","").equals("+1")) {
                increment = "++";
            }
            else if (step.replace(" ","").equals("-1")) {
                increment = "--";
            }
            else if (step.isEmpty()) {
                increment = "++";
            }
            else {
                increment = " = " + iterator + step;
            }
            iterator = iterator.trim();
            startValue = startValue.trim();
            return "for (" + iterator + " = " + translateSentence(startValue) + "; "
                            + iterator + " <= " + translateSentence(endValue) + "; "
                            + iterator + increment + ") {"
                            + comments + newline;
        }
    }

    private String translateWendSentence(String strLine) {
        int startComment = getStartComment(strLine);
        if (startComment >= 0) {
            String comments = "";
            comments =  "//" + strLine.substring(startComment);
            return "}" + comments + newline;
        }
        else {
            return "}" + newline;
        }
    }

    private String translateNextSentence(String strLine) {

        m_iteratorIndex--;

        int startComment = getStartComment(strLine);
        if (startComment >= 0) {
            String comments = "";
            comments =  "//" + strLine.substring(startComment);
            return "}" + comments + newline;
        }
        else {
            return "}" + newline;
        }
    }

    private String translateSentenceWithNewLine(String strLine) {
        return translateSentenceWithColon(strLine) + newline;
    }

    private String translateSentenceWithColon(String strLine) {
        return translateSentence(strLine) + ";";
    }

    private String translateSentence(String strLine) {
        strLine = G.ltrimTab(strLine);
        int startComment = getStartComment(strLine);
        if (G.beginLike(strLine,m_vbFunctionName + " = ")) {
            if (startComment > 0) {
                String comments = "";
                comments =  " //" + strLine.substring(startComment);
                strLine = "return "
                            + strLine.substring((m_vbFunctionName + " = ").length()
                                                , startComment).trim()
                            + ";"
                            + comments;
            }
            else {
                strLine = "return "
                            + strLine.substring((m_vbFunctionName + " = ").length());
            }
        }
        if (G.beginLike(strLine,"Set " + m_vbFunctionName + " = ")) {
            if (startComment > 0) {
                String comments = "";
                comments =  " //" + strLine.substring(startComment);
                strLine = "return "
                            + strLine.substring(("Set " + m_vbFunctionName + " = ").length() 
                                                , startComment).trim()
                            + ";"
                            + comments;
            }
            else {
                strLine = "return "
                            + strLine.substring(("Set " + m_vbFunctionName + " = ").length());
            }
        }
        if (G.beginLike(strLine,"Set ")) {
            strLine = strLine.substring(4);
        }
        if (G.beginLike(strLine,"Let ")) {
            strLine = strLine.substring(4);
        }
        strLine = replaceMemberVariables(strLine);
        strLine = replaceFunctionVariables(strLine);
        strLine = replaceAmpersand(strLine);
        strLine = replaceMidSentence(strLine);
        strLine = replaceLeftSentence(strLine);
        strLine = replaceRightSentence(strLine);
        strLine = replaceLCaseSentence(strLine);
        strLine = replaceUCaseSentence(strLine);
        strLine = replaceLenSentence(strLine);
        strLine = replaceStringComparison(strLine, "==");
        strLine = replaceStringComparison(strLine, "!=");
        strLine = replaceVbWords(strLine);
        strLine = replaceIsNothing(strLine);
        strLine = translateFunctionCall(strLine);
        strLine = replaceWithSentence(strLine);
        strLine = replaceEndWithSentence(strLine);
        strLine = replaceVbNameWithJavaName(strLine);
        strLine = replaceExitSentence(strLine);
        strLine = replaceSlashInLiterals(strLine);

        // this call has to be the last sentences in this function
        // all the changes have to be done before this call
        //
        strLine = checkEventVariableInitialization(strLine);

        return strLine;
    }

    // if the function is an event handler we will call
    // to this function in the anonymous inner class
    // which extends the adapter class of the event listener
    //
    private void checkEventHandler(String strLine) {
        // in vb all event handler functions have an
        // underscore which divide the name of the
        // variable and the name of the event
        //
        if (m_vbFunctionName.indexOf("_") > 0) {
            int i = 0;
            for (i = m_vbFunctionName.length() - 1; i > 0; i--) {
                if (m_vbFunctionName.charAt(i) == '_') {
                    break;
                }
            }
            if (i > 0) {
                String variable = m_vbFunctionName.substring(0, i);
                Iterator itrListener = null;
                itrListener = m_eventListeners.iterator();
                while(itrListener.hasNext()) {
                    EventListener listener = (EventListener)itrListener.next();
                    if (variable.equals(listener.getGenerator())) {
                        listener.getSourceCode().append(
                                getEventHandlerDeclaration(strLine));
                        break;
                    }
                }
            }
        }
    }

    private String getEventHandlerDeclaration(String strLine) {
        String handler = "";
        int i = strLine.indexOf("(");
        if (i > 0) {
            int j = 0;
            for (j = i; j > 0; j--) {
                if (strLine.charAt(j) == ' ') {
                    break;
                }
            }
            if (j > 0) {
                String functionCall = strLine.substring(j + 1, i);
                for (j = functionCall.length() - 1; j > 0; j--) {
                    if (functionCall.charAt(j) == '_') {
                        break;
                    }
                }
                String functionName = functionCall.substring(j + 1);
                j = strLine.indexOf(")");
                String params = "";
                String paramsCall = "";
                // check for empty params eg: function()
                if (j - i > 1) {
                    params = strLine.substring(i + 1, j);
                    String[] words = G.split3(params, ",");
                    for (i = 0; i < words.length; i++) {
                        j = words[i].trim().indexOf(" ");
                        paramsCall += words[i].substring(j) + ",";
                    }
                    if (paramsCall.length() > 0)
                        paramsCall = paramsCall.substring(0, paramsCall.length() - 1);
                }
                handler = "public void " + functionName + "(" + params  + ") {"
                            + newline
                            + "    " + functionCall + "(" + paramsCall + ");"
                            + newline
                            + "}" + newline;
            }
        }
        if (handler.isEmpty())
            handler = "//*TODO:**the event handler couldn't be translated: "
                            + strLine + newline;
        return handler;
    }

    private String checkEventVariableInitialization(String strLine) {
        int i = strLine.toLowerCase().indexOf(" = new ");
        if (i > 0) {
            String variable = strLine.substring(0, i).trim();
            Variable var = getMemberVariable(variable);
            if (var != null) {
                if (var.isEventGenerator) {
                    strLine += newline 
                                + getTabs()
                                + getEventMacroName(var.getJavaName());
                }
            }
        }
        return strLine;
    }

    private String replaceSlashInLiterals(String strLine) {
        boolean literalFlag = false;
        String workLine = "";
        for (int i = 0; i < strLine.length(); i++) {
            if (strLine.charAt(i) == '"') {
                literalFlag = !literalFlag;
            }
            if (literalFlag) {
                if (strLine.charAt(i) == '\\') {
                    workLine += "\\\\";
                }
                else {
                    workLine += String.valueOf(strLine.charAt(i));
                }
            }
            else {
                workLine += String.valueOf(strLine.charAt(i));
            }
        }
        return workLine;
    }

    private String replaceExitSentence(String strLine) {
        if (G.endLike(strLine, "Exit Function")) {
            return "return " + m_returnValue;
        }
        else if (G.endLike(strLine, "Exit Sub")) {
            return "return";
        }
        else if (G.endLike(strLine, "Exit For")) {
            return "break";
        }
        else if (G.endLike(strLine, "Exit Do")) {
            return "break";
        }
        else {
            return strLine;
        }
    }

    private String replaceVbNameWithJavaName(String strLine) {
        IdentifierInfo info = null;
        String type = "";
        String parent = "";
        String[] words = G.split2(strLine, "\t/*-+ .()");
        strLine = "";
        String[] parents = new String[30]; // why 30? how nows :P, 30 should be enough :)
        int openParentheses = 0;

        for (int i = 0; i < words.length; i++) {
            if (!(",.()\"'".contains(words[i]))) {
                info = getIdentifierInfo(words[i], parent);
                if (info == null)
                    type = "";
                else if (info.isFunction) {
                    type = info.function.getReturnType().dataType;
                    words[i] = info.function.getJavaName();
                    if (i + 1 < words.length) {
                        if (!words[i + 1].equals("("))
                            words[i] += "()";
                    }
                    else {
                        words[i] += "()";
                    }
                }
                else {
                    if (info.variable.isArray) {
                        int arrayParentheses = 0;
                        for (int k = i + 1; k < words.length; k++) {
                            if (words[k].equals("(")) {
                                if (arrayParentheses == 0) {
                                    words[k] = "[";
                                }
                                arrayParentheses++;
                            }
                            else if (words[k].equals(")")) {
                                arrayParentheses--;
                                if (arrayParentheses == 0) {
                                    words[k] = "]";
                                    break;
                                }
                            }
                            else if (arrayParentheses == 0
                                    && !"\t ".contains(words[k])) {
                                break;
                            }
                        }
                    }
                    type = info.variable.dataType;
                    words[i] = info.variable.getJavaName();
                }
                parent = type;
            }
            else if (words[i].equals("(")) {
                parents[openParentheses] = parent;
                openParentheses++;
            }
            else if (words[i].equals(")")) {
                openParentheses--;
                parent = parents[openParentheses];
            }
            strLine += words[i];
        }
        return strLine;
    }

    private String replaceIsNothing(String strLine) {
        return strLine.replaceAll("Is Nothing", "== null");
    }

    private String replaceWithSentence(String strLine) {
        if (G.beginLike(strLine, "with ")) {
            m_withDeclaration = true;

            // first we have to get the variable
            // and then the type of it
            //
            int startComment = getStartComment(strLine);
            String workLine = strLine;
            String comments = "";
            if (startComment >= 0) {
                comments =  "; //" + workLine.substring(startComment);
                workLine = workLine.substring(0, startComment-1);
            }
            int i = workLine.toLowerCase().indexOf("with");
            IdentifierInfo info = null;
            String packageName = "";
            String type = "";
            String parent = "";
            workLine = workLine.substring(i + 5).trim();
            if (workLine.charAt(0) == '.') {
                if (workLine.length() > 1) {
                    workLine = workLine.substring(1);
                }
                else {
                    workLine = "";
                }
            }
            String[] words = G.split3(workLine,".");
            if (m_collWiths.size() > 0) {
                parent = m_collWiths.get(m_collWiths.size()-1).dataType;
            }
            for (i = 0; i < words.length; i++) {
                info = getIdentifierInfo(words[i], parent);
                if (info == null)
                    type = "";
                else if (info.isFunction)
                    type = info.function.getReturnType().dataType;
                else
                    type = info.variable.dataType;
                parent = type;
            }
            String parentWithCall = ""; // for example: "tBi." in "With tBI.bmiHeader"
            for (i = 0; i < words.length-1; i++) {
                parentWithCall += words[i] + ".";
            }

            String prefix = "";
            if (type.length() == 0) {
                type = "__TYPE_NOT_FOUND";
                prefix = "//*TODO:** can't found type for with block"
                            + newline
                            + getTabs()
                            + "//*"
                            + strLine
                            + newline
                            + getTabs();
            }

            Variable var = new Variable();
            var.setType(type);
            var.packageName = packageName;

            if (info == null) {
                var.setJavaName("w_" + var.dataType.substring(0,1).toLowerCase()
                            + var.dataType.substring(1));

                if (m_inWith) {
                    strLine = prefix
                                + var.dataType
                                + " "
                                + var.getJavaName()
                                + " = "
                                + m_collWiths.get(m_collWiths.size()-1).getJavaName()
                                + "."
                                + workLine//.substring(5)
                                + comments;
                }
                else {
                    strLine = prefix
                                + var.dataType
                                + " "
                                + var.getJavaName()
                                + " = " + workLine//.substring(5)
                                + comments;
                    m_inWith = true;
                }
            }
            else {
                if (info.isFunction) {
                    var.setJavaName("w_" + info.function.getVbName().substring(0,1).toLowerCase()
                                + info.function.getVbName().substring(1));
                    String params = "";
                    int startParams = workLine.indexOf("(");
                    if (startParams >= 0) {
                        params = workLine.substring(startParams);
                    }
                    else {
                        params = "()";
                    }
                    if (m_inWith) {
                        strLine = prefix
                                    + var.dataType
                                    + " "
                                    + var.getJavaName()
                                    + " = "
                                    + m_collWiths.get(m_collWiths.size()-1).getJavaName()
                                    + "."
                                    + info.function.getJavaName()
                                    + params
                                    + comments;
                    }
                    else {
                        strLine = prefix
                                    + var.dataType
                                    + " "
                                    + var.getJavaName()
                                    + " = "
                                    + parentWithCall
                                    + info.function.getJavaName()
                                    + params
                                    + comments;
                        m_inWith = true;
                    }
                }
                else {
                    String arrayIndex = "";
                    if (info.variable.isArray) {
                        int startArrayIndex = workLine.indexOf("(");
                        if (startArrayIndex >= 0) {
                            arrayIndex = workLine.substring(startArrayIndex).replace("(","[").replace(")","]");
                        }
                    }
                    var.setJavaName(parentWithCall + info.variable.getJavaName() + arrayIndex);
                    strLine = "// " + strLine;
                    m_inWith = true;
                }
            }
            m_collWiths.add(var);
        }
        else {
            m_withDeclaration = false;
        }
        return strLine;
    }

    private String replaceEndWithSentence(String strLine) {
        boolean isEndWith = false;
        if (strLine.equalsIgnoreCase("end with")) {
            isEndWith = true;
        }
        if (G.beginLike(strLine, "end with ")) {
            isEndWith = true;
        }
        m_endWithDeclaration = isEndWith;
        if (isEndWith) {
            String withName = "";
            if (m_collWiths.size() > 0) {
                withName = m_collWiths.get(m_collWiths.size()-1).getJavaName();
                m_collWiths.remove(m_collWiths.size()-1);
            }
            m_inWith = m_collWiths.size() > 0;
            return "// {end with: " + withName + "}";
        }
        else
            return strLine;
    }

    private IdentifierInfo getIdentifierInfo(String identifier, String parent) {
        // - get the object from this class (member variables)
        // if the object is not found then
        // - get the object from the database (public variables)
        //      -- first objects in this package then objects in
        //         other packages in the order set in the vbp's
        //         reference list
        IdentifierInfo info = null;
        Variable var = getVariable(identifier, parent);
        if (var != null) {
            info = new IdentifierInfo();
            info.isFunction = false;
            info.variable = var;
        }
        else {
            Function function = getFunction(identifier, parent);
            if (function != null) {
                info = new IdentifierInfo();
                info.isFunction = true;
                info.function = function;
            }
        }
        return info;
    }

    private String translateFunctionCall(String strLine) {

        // we will process with later
        //
        if (G.beginLike(strLine,"With ")) {
            return strLine;
        }

        if (G.beginLike(strLine,"Call ")) {
            strLine = strLine.substring(5);
        }

        int startComment = getStartComment(strLine);
        String workLine = strLine;
        String comments = "";
        if (startComment >= 0) {
            comments =  "//" + workLine.substring(startComment);
            workLine = workLine.substring(0, startComment-1);
        }
        String[] words = getWordsFromSentence(workLine);
        if (words.length >= 2) {
            if (!words[0].equals("return")) {
                if (!words[0].equals("(") && !words[1].equals("(") && !words[2].equals("(")) {
                    if (!C_SEPARARTORS.contains("_" + words[2] + "_")) {
                        if (!isReservedWord(words[0])) {
                            strLine = words[0] + "(";
                            String params = "";
                            for (int i = 1; i < words.length; i++) {
                                params += words[i];
                            }
                            strLine += params.trim() + ")" + comments;
                        }
                    }
                }
                // special case when in vb whe have a SUB call
                // with only one parameter sourronded by parentheses
                // like Col.Remove (Ctrl.Key)
                //
                else if (words[1].equals(" ") && words[2].equals("(")) {
                    if (!C_SEPARARTORS.contains("_" + words[2] + "_")) {
                        if (!isReservedWord(words[0])) {
                            strLine = words[0];
                            String params = "";
                            for (int i = 2; i < words.length; i++) {
                                params += words[i];
                            }
                            strLine += params.trim() + comments;
                        }
                    }
                }
            }
        }
        return strLine;
    }

    private boolean isReservedWord(String word) {
        return (C_RESERVED_WORDS.contains("_" + word.toLowerCase() + "_"));
    }

    private String replaceMemberVariables(String strLine) {
        boolean found = false;
        String rtn = "";
        String[] words = getWordsFromSentence(strLine);

        for (int i = 0; i < words.length; i++) {
            found = false;
            for (int j = 0; j < m_memberVariables.size(); j++) {
                /*System.out.println(m_memberVariables.get(j)
                        + "    " + words[i]
                        );*/
                if (words[i].equalsIgnoreCase(m_memberVariables.get(j).getJavaName())) {
                    rtn += m_memberVariables.get(j).getJavaName();
                    found = true;
                    break;
                }
            }
            if (!found) {
                rtn += words[i];
            }
        }
        return rtn;
    }

    private String replaceFunctionVariables(String strLine) {
        boolean found = false;
        String rtn = "";
        String[] words = getWordsFromSentence(strLine);

        for (int i = 0; i < words.length; i++) {
            found = false;
            for (int j = 0; j < m_functionVariables.size(); j++) {
                /*System.out.println(m_functionVariables.get(j)
                        + "    " + words[i]
                        );*/
                if (words[i].equalsIgnoreCase(m_functionVariables.get(j).getJavaName())) {
                    rtn += m_functionVariables.get(j).getJavaName();
                    found = true;
                    break;
                }
            }
            if (!found) {
                rtn += words[i];
            }
        }
        return rtn;
    }

    private String replaceAmpersand(String strLine) {
        boolean ampFound = false;
        String rtn = "";
        String[] words = getWordsFromSentence(strLine);

        for (int i = 0; i < words.length; i++) {
            if (words[i].equals("&")) {
                rtn += "+";
                ampFound = true;
            }
            else {
                if (ampFound) {
                    if (words[i].equals(" "))
                        rtn += " ";
                    else {
                        rtn += getCastToString(words[i]);
                        ampFound = false;
                    }
                }
                else {
                    if (i < words.length-1) {
                        if (words[i+1].equals("&")) {
                            rtn += getCastToString(words[i]);
                        }
                        else {
                            if (i < words.length-2) {
                                if (words[i+2].equals("&")) {
                                    rtn += getCastToString(words[i]);
                                }
                                else {
                                    rtn += words[i];
                                }
                            }
                            else {
                                rtn += words[i];
                            }
                        }
                    }
                    else {
                        rtn += words[i];
                    }
                }
            }
        }
        return rtn;
    }

    private String replaceStringComparison(String strLine, String operator) {
        boolean equalsFound = false;
        boolean innerEqualFound = false;
        int openParentheses = 0;
        String innerParentheses = "";
        String firstOperand = "";
        String secondOperand = "";
        String rtn = "";
        String[] words = getWordsFromSentence(strLine);

        // first we have to serch for the operands
        //
        for (int i = 0; i < words.length; i++) {
            if (equalsFound) {
                // if we are sourronded by parentheses
                //
                if (openParentheses > 0) {
                    if (words[i].equals(")")) {
                        openParentheses--;
                        if (openParentheses == 0) {
                            if (innerEqualFound) {
                                innerEqualFound = false;
                                innerParentheses = replaceStringComparison(
                                                        innerParentheses,
                                                        operator);
                            }
                            secondOperand += "(" + G.ltrim(innerParentheses) + ")";
                            innerParentheses = "";
                        }
                        else
                            innerParentheses += ")";
                    }
                    else {
                        if (words[i].equals("(")) {
                            openParentheses++;
                        } else if (words[i].equals(operator)) {
                            innerEqualFound = true;
                        }
                        innerParentheses += words[i];
                    }
                }
                // there isn't any open parentheses
                //
                else {
                    if (words[i].equals("(")) {
                        openParentheses = 1;
                        innerParentheses = "";
                    }
                    else if (words[i].equals("&&")) {
                        rtn += processEqualsSentence(
                                    firstOperand,
                                    secondOperand,
                                    operator) + " &&";
                        equalsFound = false;
                        firstOperand = "";
                        secondOperand = "";
                    }
                    else if (words[i].equals("||")) {
                        rtn += processEqualsSentence(
                                    firstOperand,
                                    secondOperand,
                                    operator) + " ||";
                        equalsFound = false;
                        firstOperand = "";
                        secondOperand = "";
                    }
                    else
                        secondOperand += words[i];
                }
            }
            else {
                // if we are sourronded by parentheses
                //
                if (openParentheses > 0) {
                    if (words[i].equals(")")) {
                        openParentheses--;
                        if (openParentheses == 0) {
                            if (innerEqualFound) {
                                innerEqualFound = false;
                                innerParentheses = replaceStringComparison(
                                                        innerParentheses,
                                                        operator);
                            }
                            firstOperand += "(" + G.ltrim(innerParentheses) + ")";
                            innerParentheses = "";
                        }
                        else
                            innerParentheses += ")";
                    }
                    else {
                        if (words[i].equals("(")) {
                            openParentheses++;
                        } else if (words[i].equals(operator)) {
                            innerEqualFound = true;
                        }
                        innerParentheses += words[i];
                    }
                }

                // there isn't any open parentheses
                //
                else {
                    // if we found an left (open) parentheses
                    //
                    if (words[i].equals("(")) {
                        openParentheses = 1;
                        innerParentheses = "";
                    }
                    else if (words[i].equals(operator)) {
                        equalsFound = true;
                        innerParentheses = "";
                    }
                    else if (words[i].equals("&&")) {
                        rtn += firstOperand + "&&";
                        firstOperand = "";
                    }
                    else if (words[i].equals("||")) {
                        rtn += firstOperand + "||";
                        firstOperand = "";
                    }
                    else
                        firstOperand += words[i];
                }
            }
        }

        if (!firstOperand.isEmpty()) {
            if (!secondOperand.isEmpty())
                rtn += processEqualsSentence(
                            firstOperand,
                            secondOperand,
                            operator);
            else
                rtn += firstOperand;
        }
        return rtn;
    }

    private String replaceMidSentence(String expression) {
        boolean midFound = false;

        expression = G.ltrimTab(expression);

        if (containsMid(expression)) {

            int openParentheses = 0;
            String[] words = G.split(expression);
            String params = "";
            expression = "";
            midFound = false;

            for (int i = 0; i < words.length; i++) {
                if (midFound) {
                    if (words[i].equals("(")) {
                        openParentheses++;
                        if (openParentheses > 1) {
                            params += words[i];
                        }
                    }
                    // look for a close parentheses without an open parentheses
                    else if (words[i].equals(")")) {
                        openParentheses--;
                        if (openParentheses == 0) {
                            if (containsMid(params)) {
                                params = replaceMidSentence(params);
                            }
                            String[] vparams = G.split(params);
                            String identifier = "";
                            String start = "";
                            String end = "";

                            int colons = 0;
                            identifier = "";
                            for (int t = 0; t < vparams.length; t++) {
                                if (vparams[t].equals(",")) {
                                    colons++;
                                }
                                else {

                                    if (colons == 0) {
                                        identifier += vparams[t];
                                    }
                                    else if (colons == 1) {
                                        start += vparams[t];
                                    }
                                    else if (colons == 2) {
                                        end += vparams[t];
                                    }
                                    else {
                                        G.showInfo("Unexpected colon found in Mid function's params: " + params);
                                    }
                                }
                            }
                            // identifier can be a complex expresion
                            // like ' "an string plus" + a_var '
                            //
                            if (G.contains(identifier, " ")) {
                                identifier = "(" + identifier + ")";
                            }
                            expression += identifier + ".substring(" + start.trim();
                            if (!end.isEmpty()) {
                                expression += ", " + end.trim() + ")";
                            }
                            else {
                                expression += ")";
                            }
                            midFound = false;
                            params = "";
                        }
                        else {
                            params = params.trim() + words[i];
                        }
                    }
                    else {
                        params += words[i];
                    }
                }
                else {
                    if (words[i].equalsIgnoreCase("mid")) {
                        midFound = true;
                    }
                    else if (words[i].equalsIgnoreCase("mid$")) {
                        midFound = true;
                    }
                    else if (G.beginLike(words[i],"mid(")) {
                        expression += replaceMidSentence(words[i]);
                    }
                    else if (G.beginLike(words[i],"mid$(")) {
                        expression += replaceMidSentence(words[i]);
                    }
                    else {
                        expression += words[i];
                    }
                }
            }
        }
        return expression.trim();
    }

    private String replaceLeftSentence(String expression) {
        boolean leftFound = false;

        expression = G.ltrimTab(expression);

        if (containsLeft(expression)) {

            int openParentheses = 0;
            String[] words = G.split(expression);
            String params = "";
            expression = "";
            leftFound = false;

            for (int i = 0; i < words.length; i++) {
                if (leftFound) {
                    if (words[i].equals("(")) {
                        openParentheses++;
                        if (openParentheses > 1) {
                            params += words[i];
                        }
                    }
                    // look for a close parentheses without an open parentheses
                    else if (words[i].equals(")")) {
                        openParentheses--;
                        if (openParentheses == 0) {
                            if (containsLeft(params)) {
                                params = replaceLeftSentence(params);
                            }
                            String[] vparams = G.split(params);
                            String identifier = "";
                            String length = "";

                            int colons = 0;
                            identifier = "";
                            for (int t = 0; t < vparams.length; t++) {
                                if (vparams[t].equals(",")) {
                                    colons++;
                                }
                                else {

                                    if (colons == 0) {
                                        identifier += vparams[t];
                                    }
                                    else if (colons == 1) {
                                        length += vparams[t];
                                    }
                                    else {
                                        G.showInfo("Unexpected colon found in Left function's params: " + params);
                                    }
                                }
                            }
                            // identifier can be a complex expresion
                            // like ' "an string plus" + a_var '
                            //
                            if (G.contains(identifier, " ")) {
                                identifier = "(" + identifier + ")";
                            }
                            expression += identifier
                                            + ".substring(0, " + length.trim()+ ")";
                            leftFound = false;
                            params = "";
                        }
                        else {
                            params = params.trim() + words[i];
                        }
                    }
                    else {
                        params += words[i];
                    }
                }
                else {
                    if (words[i].equalsIgnoreCase("left")) {
                        leftFound = true;
                    }
                    else if (words[i].equalsIgnoreCase("left$")) {
                        leftFound = true;
                    }
                    else if (G.beginLike(words[i],"left(")) {
                        expression += replaceLeftSentence(words[i]);
                    }
                    else if (G.beginLike(words[i],"left$(")) {
                        expression += replaceLeftSentence(words[i]);
                    }
                    else {
                        expression += words[i];
                    }
                }
            }
        }
        return expression.trim();
    }

    private String replaceRightSentence(String expression) {
        boolean rightFound = false;

        expression = G.ltrimTab(expression);

        if (containsRight(expression)) {

            int openParentheses = 0;
            String[] words = G.split(expression);
            String params = "";
            expression = "";
            rightFound = false;

            for (int i = 0; i < words.length; i++) {
                if (rightFound) {
                    if (words[i].equals("(")) {
                        openParentheses++;
                        if (openParentheses > 1) {
                            params += words[i];
                        }
                    }
                    // look for a close parentheses without an open parentheses
                    else if (words[i].equals(")")) {
                        openParentheses--;
                        if (openParentheses == 0) {
                            if (containsRight(params)) {
                                params = replaceRightSentence(params);
                            }
                            String[] vparams = G.split(params);
                            String identifier = "";
                            String lenght = "";

                            int colons = 0;
                            identifier = "";
                            for (int t = 0; t < vparams.length; t++) {
                                if (vparams[t].equals(",")) {
                                    colons++;
                                }
                                else {

                                    if (colons == 0) {
                                        identifier += vparams[t];
                                    }
                                    else if (colons == 1) {
                                        lenght += vparams[t];
                                    }
                                    else {
                                        G.showInfo("Unexpected colon found in Right function's params: " + params);
                                    }
                                }
                            }
                            // identifier can be a complex expresion
                            // like ' "an string plus" + a_var '
                            //
                            if (G.contains(identifier, " ")) {
                                identifier = "(" + identifier + ")";
                            }
                            expression += identifier
                                            + ".substring(" + identifier
                                            + ".length() - " + lenght.trim() + ")";
                            rightFound = false;
                            params = "";
                        }
                        else {
                            params = params.trim() + words[i];
                        }
                    }
                    else {
                        params += words[i];
                    }
                }
                else {
                    if (words[i].equalsIgnoreCase("right")) {
                        rightFound = true;
                    }
                    else if (words[i].equalsIgnoreCase("right$")) {
                        rightFound = true;
                    }
                    else if (G.beginLike(words[i],"right(")) {
                        expression += replaceRightSentence(words[i]);
                    }
                    else if (G.beginLike(words[i],"right$(")) {
                        expression += replaceRightSentence(words[i]);
                    }
                    else {
                        expression += words[i];
                    }
                }
            }
        }
        return expression.trim();
    }

    private String replaceLCaseSentence(String expression) {
        boolean lcaseFound = false;

        expression = G.ltrimTab(expression);

        if (containsLCase(expression)) {

            int openParentheses = 0;
            String[] words = G.split(expression);
            String params = "";
            expression = "";
            lcaseFound = false;

            for (int i = 0; i < words.length; i++) {
                if (lcaseFound) {
                    if (words[i].equals("(")) {
                        openParentheses++;
                        if (openParentheses > 1) {
                            params += words[i];
                        }
                    }
                    // look for a close parentheses without an open parentheses
                    else if (words[i].equals(")")) {
                        openParentheses--;
                        if (openParentheses == 0) {
                            if (containsLCase(params)) {
                                params = replaceLCaseSentence(params);
                            }
                            String[] vparams = G.split(params);
                            String identifier = "";

                            int colons = 0;
                            identifier = "";
                            for (int t = 0; t < vparams.length; t++) {
                                if (vparams[t].equals(",")) {
                                    colons++;
                                }
                                else {

                                    if (colons == 0) {
                                        identifier += vparams[t];
                                    }
                                    else {
                                        G.showInfo("Unexpected colon found in LCase function's params: " + params);
                                    }
                                }
                            }
                            // identifier can be a complex expresion
                            // like ' "an string plus" + a_var '
                            //
                            if (G.contains(identifier, " ")) {
                                identifier = "(" + identifier + ")";
                            }
                            expression += identifier
                                            + ".toLowerCase()";
                            lcaseFound = false;
                            params = "";
                        }
                        else {
                            params = params.trim() + words[i];
                        }
                    }
                    else {
                        params += words[i];
                    }
                }
                else {
                    if (words[i].equalsIgnoreCase("lcase")) {
                        lcaseFound = true;
                    }
                    else if (words[i].equalsIgnoreCase("lcase$")) {
                        lcaseFound = true;
                    }
                    else if (G.beginLike(words[i],"lcase(")) {
                        expression += replaceLCaseSentence(words[i]);
                    }
                    else if (G.beginLike(words[i],"lcase$(")) {
                        expression += replaceLCaseSentence(words[i]);
                    }
                    else {
                        expression += words[i];
                    }
                }
            }
        }
        return expression.trim();
    }

    private String replaceUCaseSentence(String expression) {
        boolean ucaseFound = false;

        expression = G.ltrimTab(expression);

        if (containsUCase(expression)) {

            int openParentheses = 0;
            String[] words = G.split(expression);
            String params = "";
            expression = "";
            ucaseFound = false;

            for (int i = 0; i < words.length; i++) {
                if (ucaseFound) {
                    if (words[i].equals("(")) {
                        openParentheses++;
                        if (openParentheses > 1) {
                            params += words[i];
                        }
                    }
                    // look for a close parentheses without an open parentheses
                    else if (words[i].equals(")")) {
                        openParentheses--;
                        if (openParentheses == 0) {
                            if (containsUCase(params)) {
                                params = replaceUCaseSentence(params);
                            }
                            String[] vparams = G.split(params);
                            String identifier = "";

                            int colons = 0;
                            identifier = "";
                            for (int t = 0; t < vparams.length; t++) {
                                if (vparams[t].equals(",")) {
                                    colons++;
                                }
                                else {

                                    if (colons == 0) {
                                        identifier += vparams[t];
                                    }
                                    else {
                                        G.showInfo("Unexpected colon found in UCase function's params: " + params);
                                    }
                                }
                            }
                            // identifier can be a complex expresion
                            // like ' "an string plus" + a_var '
                            //
                            if (G.contains(identifier, " ")) {
                                identifier = "(" + identifier + ")";
                            }
                            expression += identifier
                                            + ".toUpperCase()";
                            ucaseFound = false;
                            params = "";
                        }
                        else {
                            params = params.trim() + words[i];
                        }
                    }
                    else {
                        params += words[i];
                    }
                }
                else {
                    if (words[i].equalsIgnoreCase("ucase")) {
                        ucaseFound = true;
                    }
                    else if (words[i].equalsIgnoreCase("ucase$")) {
                        ucaseFound = true;
                    }
                    else if (G.beginLike(words[i],"ucase(")) {
                        expression += replaceUCaseSentence(words[i]);
                    }
                    else if (G.beginLike(words[i],"ucase$(")) {
                        expression += replaceUCaseSentence(words[i]);
                    }
                    else {
                        expression += words[i];
                    }
                }
            }
        }
        return expression.trim();
    }

    private String replaceLenSentence(String expression) {
        boolean lenFound = false;

        expression = G.ltrimTab(expression);

        if (containsLen(expression)) {

            int openParentheses = 0;
            String[] words = G.split(expression);
            String params = "";
            expression = "";
            lenFound = false;

            for (int i = 0; i < words.length; i++) {
                if (lenFound) {
                    if (words[i].equals("(")) {
                        openParentheses++;
                        if (openParentheses > 1) {
                            params += words[i];
                        }
                    }
                    // look for a close parentheses without an open parentheses
                    else if (words[i].equals(")")) {
                        openParentheses--;
                        if (openParentheses == 0) {
                            if (containsLen(params)) {
                                params = replaceLenSentence(params);
                            }
                            String[] vparams = G.split(params);
                            String identifier = "";

                            int colons = 0;
                            identifier = "";
                            for (int t = 0; t < vparams.length; t++) {
                                if (vparams[t].equals(",")) {
                                    colons++;
                                }
                                else {

                                    if (colons == 0) {
                                        identifier += vparams[t];
                                    }
                                    else {
                                        G.showInfo("Unexpected colon found in Len function's params: " + params);
                                    }
                                }
                            }
                            // identifier can be a complex expresion
                            // like ' "an string plus" + a_var '
                            //
                            if (G.contains(identifier, " ")) {
                                identifier = "(" + identifier + ")";
                            }
                            expression += identifier + ".lenght()";
                            lenFound = false;
                            params = "";
                        }
                        else {
                            params = params.trim() + words[i];
                        }
                    }
                    else {
                        params += words[i];
                    }
                }
                else {
                    if (words[i].equalsIgnoreCase("len")) {
                        lenFound = true;
                    }
                    else if (G.beginLike(words[i],"len(")) {
                        expression += replaceLenSentence(words[i]);
                    }
                    else if (words[i].toLowerCase().contains(" len(")) {
                        expression += replaceLenSentence(words[i]);
                    }
                    else if (words[i].toLowerCase().contains("(len(")) {
                        expression += replaceLenSentence(words[i]);
                    }
                    else {
                        expression += words[i];
                    }
                }
            }
        }
        return expression.trim();
    }

    private boolean containsMid(String expression) {
        if (expression.toLowerCase().contains(" mid(")) {
            return true;
        }
        else if (expression.toLowerCase().contains("(mid(")) {
            return true;
        }
        else if (expression.toLowerCase().contains(" mid$(")) {
            return true;
        }
        else if (expression.toLowerCase().contains("(mid$(")) {
            return true;
        } 
        else if (G.beginLike(expression,"mid(")) {
            return true;
        }
        else if (G.beginLike(expression,"mid$(")) {
            return true;
        }
        else {
            return false;
        }
    }

    private boolean containsLeft(String expression) {
        if (expression.toLowerCase().contains(" left(")) {
            return true;
        }
        else if (expression.toLowerCase().contains("(left(")) {
            return true;
        }
        else if (expression.toLowerCase().contains(" left$(")) {
            return true;
        }
        else if (expression.toLowerCase().contains("(left$(")) {
            return true;
        }
        else if (G.beginLike(expression,"left(")) {
            return true;
        }
        else if (G.beginLike(expression,"left$(")) {
            return true;
        }
        else {
            return false;
        }
    }

    private boolean containsRight(String expression) {
        if (expression.toLowerCase().contains(" right(")) {
            return true;
        }
        else if (expression.toLowerCase().contains("(right(")) {
            return true;
        }
        else if (expression.toLowerCase().contains(" right$(")) {
            return true;
        }
        else if (expression.toLowerCase().contains("(right$(")) {
            return true;
        }
        else if (G.beginLike(expression,"right(")) {
            return true;
        }
        else if (G.beginLike(expression,"right$(")) {
            return true;
        }
        else {
            return false;
        }
    }

    private boolean containsLCase(String expression) {
        if (expression.toLowerCase().contains(" lcase(")) {
            return true;
        }
        else if (expression.toLowerCase().contains("(lcase(")) {
            return true;
        }
        else if (expression.toLowerCase().contains(" lcase$(")) {
            return true;
        }
        else if (expression.toLowerCase().contains("(lcase$(")) {
            return true;
        }
        else if (G.beginLike(expression,"lcase(")) {
            return true;
        }
        else if (G.beginLike(expression,"lcase$(")) {
            return true;
        }
        else {
            return false;
        }
    }

    private boolean containsUCase(String expression) {
        if (expression.toLowerCase().contains(" ucase(")) {
            return true;
        }
        else if (expression.toLowerCase().contains("(ucase(")) {
            return true;
        }
        else if (expression.toLowerCase().contains(" ucase$(")) {
            return true;
        }
        else if (expression.toLowerCase().contains("(ucase$(")) {
            return true;
        }
        else if (G.beginLike(expression,"ucase(")) {
            return true;
        }
        else if (G.beginLike(expression,"ucase$(")) {
            return true;
        }
        else {
            return false;
        }
    }

    private boolean containsLen(String expression) {
        if (expression.toLowerCase().contains(" len(")) {
            return true;
        }
        else if (expression.toLowerCase().contains("(len(")) {
            return true;
        }
        else if (G.beginLike(expression,"len(")) {
            return true;
        }
        else {
            return false;
        }
    }

    private String replaceVbWords(String expression) {
        expression = G.ltrimTab(expression);
        String[] words = G.split2(expression);
        expression = "";

        // Public
        //
        for (int i = 0; i < words.length; i++) {
            if (words[i].equalsIgnoreCase("vbNullString")) {
                words[i] = "\"\"";
            }
            expression += words[i];// + " ";
        }
        return expression.trim();
    }

    private String processEqualsSentence(String firstOperand, String secondOperand, String operator) {
        boolean isNotEquals = operator.equals("!=");
        if (isStringExpression(firstOperand)) {
            if (isNotEquals)
                return "!("
                        + G.rtrim(firstOperand) + ".equals(" + secondOperand.trim() + ")"
                        +")";
            else
                return G.rtrim(firstOperand) + ".equals(" + secondOperand.trim() + ")";
        }
        else if (isStringExpression(secondOperand)) {
            if (isNotEquals)
                return "!("
                        + G.rtrim(secondOperand) + ".equals(" + firstOperand.trim() + ")"
                        + ")";
            else
                return G.rtrim(secondOperand) + ".equals(" + firstOperand.trim() + ")";
        }
        else {
            if (isNotEquals)
                return firstOperand + "!=" + secondOperand;
            else
                return firstOperand + "==" + secondOperand;
        }
    }

    private boolean isStringExpression(String expression) {
        IdentifierInfo info = null;
        String type = "";
        String parent = "";
        expression = expression.trim();
        if (expression.charAt(0) == '.') {
            if (expression.length() > 1) {
                expression = expression.substring(1);
            }
            else {
                expression = "";
            }
        }
        String[] words = G.split3(expression,".");
        if (m_collWiths.size() > 0) {
            parent = m_collWiths.get(m_collWiths.size()-1).dataType;
        }
        for (int i = 0; i < words.length; i++) {
            info = getIdentifierInfo(words[i], parent);
            if (info == null)
                type = "";
            else if (info.isFunction)
                type = info.function.getReturnType().dataType;
            else
                type = info.variable.dataType;
            parent = type;
        }

        if (info == null) {
            if (expression.charAt(0) == '"') {
                if (expression.charAt(expression.length()-1) == '"') {
                    return true;
                }
            }
            return false;
        }
        else {
            if (info.isFunction) {
                return info.function.getReturnType().isString;
            }
            else {
                return info.variable.isString;
            }
        }
    }

    private Function getFunction(String expression, String className) {
        String functionName = "";

        if (expression.contains("(")) {
            int i = expression.indexOf("(");
            if (i > 0) {
                functionName = expression.substring(0, i);
            }
        }
        else {
            functionName = expression;
        }
        if (functionName.isEmpty()) {
            return null;
        }

        Iterator itrFile = null;

        // first we search in private functions
        //
        if (className.isEmpty()
                || className.toLowerCase().equals("this")
                || className.toLowerCase().equals("me")) {
            itrFile = m_collFiles.iterator();
            while(itrFile.hasNext()) {
                SourceFile source = (SourceFile)itrFile.next();
                if (source.getJavaName().equals(m_javaClassName)) {
                    Iterator itrPrivateFunctions = source.getPrivateFunctions().iterator();
                    while (itrPrivateFunctions.hasNext()) {
                        Function privateFunction = (Function)itrPrivateFunctions.next();
                        if (privateFunction.getJavaName().equals(functionName))
                            return privateFunction;
                        else if (privateFunction.getVbName().equals(functionName))
                            return privateFunction;
                    }
                    break;
                }
            }
        }

        // here we search for public functions, public properties
        //
        itrFile = m_collFiles.iterator();
        while(itrFile.hasNext()) {
            SourceFile source = (SourceFile)itrFile.next();
            if (className.isEmpty()
                    || source.getJavaName().equals(className)
                    || source.getVbName().equals(className)) {
                Iterator itrPublicFunctions = source.getPublicFunctions().iterator();
                while (itrPublicFunctions.hasNext()) {
                    Function publicFunction = (Function)itrPublicFunctions.next();
                    if (publicFunction.getJavaName().equals(functionName))
                        return publicFunction;
                    else if (publicFunction.getVbName().equals(functionName))
                        return publicFunction;
                }
            }
        }

        // here we search for public functions in java
        //
        itrFile = m_collJavaClassess.iterator();
        while(itrFile.hasNext()) {
            SourceFile source = (SourceFile)itrFile.next();
            if (className.isEmpty()
                    || source.getJavaName().equals(className)) {
                Iterator itrPublicFunctions = source.getPublicFunctions().iterator();
                while (itrPublicFunctions.hasNext()) {
                    Function publicFunction = (Function)itrPublicFunctions.next();
                    if (publicFunction.getJavaName().equals(functionName))
                        return publicFunction;
                }
            }
        }

        // if we are here, we must look in the database
        //
        Function publicFunction = FunctionObject.getFunctionFromName(
                                                    expression,
                                                    className,
                                                    m_references);

        return publicFunction;
    }

    private String getCastToString(String identifier) {
        Variable var = getVariable(identifier);
        if (var != null) {
            if (var.isString)
                return identifier;
            else if (var.isLong)
                return "((Long) " + identifier + ").ToString()";
            else if (var.isInt)
                return "((Integer) " + identifier + ").ToString()";
            else if (var.isBoolean)
                return "((Boolean) " + identifier + ").ToString()";
            else
                return identifier;
        }
        else
            return identifier;
    }

    private Variable getVariable(String identifier) {
        return getVariable(identifier, "");
    }

    private Variable getVariable(String expression, String className) {
        String identifier = "";

        // in vb arrays use parentheses to define the index
        // eg: m_vGroups(i)
        //
        if (expression.contains("(")) {
            int i = expression.indexOf("(");
            if (i > 0) {
                identifier = expression.substring(0, i);
            }
        }
        else {
            identifier = expression;
        }
        if (identifier.isEmpty()) {
            return null;
        }

        for (int i = 0; i < m_functionVariables.size(); i++) {
            if (identifier.equals(m_functionVariables.get(i).getVbName())) {
                return m_functionVariables.get(i);
            }
            if (identifier.equals(m_functionVariables.get(i).getJavaName())) {
                return m_functionVariables.get(i);
            }
        }
        for (int i = 0; i < m_memberVariables.size(); i++) {
            if (identifier.equals(m_memberVariables.get(i).getVbName())) {
                return m_memberVariables.get(i);
            }
            if (identifier.equals(m_memberVariables.get(i).getJavaName())) {
                return m_memberVariables.get(i);
            }
        }

        // now we search in private types and public types
        // declared in this class
        //
        Iterator itrTypes = null;

        if (!className.isEmpty()) {
            itrTypes = m_types.iterator();
            while(itrTypes.hasNext()) {
                Type type = (Type)itrTypes.next();
                if (type.javaName.equals(className)
                        || type.vbName.equals(className)) {
                    Iterator itrMembers = type.getMembersVariables().iterator();
                    while (itrMembers.hasNext()) {
                        Variable member = (Variable)itrMembers.next();
                        if (member.getJavaName().equals(identifier))
                            return member;
                        else if (member.getVbName().equals(identifier))
                            return member;
                    }
                }
            }
        }

        // if we are here, we must look in the database
        //
        Variable publicVariable = VariableObject.getVariableFromName(
                                                    identifier,
                                                    className,
                                                    m_references);

        return publicVariable;
    }

    private Variable getMemberVariable(String expression) {
        String identifier = "";

        // in vb arrays use parentheses to define the index
        // eg: m_vGroups(i)
        //
        if (expression.contains("(")) {
            int i = expression.indexOf("(");
            if (i > 0) {
                identifier = expression.substring(0, i);
            }
        }
        else {
            identifier = expression;
        }
        if (identifier.isEmpty()) {
            return null;
        }

        for (int i = 0; i < m_memberVariables.size(); i++) {
            if (identifier.equals(m_memberVariables.get(i).getVbName())) {
                return m_memberVariables.get(i);
            }
            if (identifier.equals(m_memberVariables.get(i).getJavaName())) {
                return m_memberVariables.get(i);
            }
        }
        return null;
    }

    private String[] getWordsFromSentence(String strLine) {
        boolean literalFlag = false;
        String[] words = new String[500];
        String word = "";
        int j = 0;
        for (int i = 0; i < strLine.length(); i++) {
            if (strLine.charAt(i) == '"') {
                literalFlag = !literalFlag;
            }
            if (!literalFlag) {
                if (C_SYMBOLS.contains(String.valueOf(strLine.charAt(i)))) {
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

    private boolean isFunctionDeclaration(String strLine) {
        if (G.beginLike(strLine, "Public Function "))
            return true;
        else if (G.beginLike(strLine, "Private Function "))
            return true;
        else if (G.beginLike(strLine, "Friend Function "))
            return true;
        else if (G.beginLike(strLine, "Public Sub "))
            return true;
        else if (G.beginLike(strLine, "Private Sub "))
            return true;
        else if (G.beginLike(strLine, "Friend Sub "))
            return true;
        else if (G.beginLike(strLine, "Public Property "))
            return true;
        else if (G.beginLike(strLine, "Private Property "))
            return true;
        else if (G.beginLike(strLine, "Friend Property "))
            return true;
        else
            return false;
    }

    private String translateFunctionDeclaration(String strLine) {
        String functionName = "";
        String functionType = "";
        String functionScope = "";

        m_vbFunctionName = "";
        m_functionVariables.removeAll(m_functionVariables);

        strLine = G.ltrimTab(strLine);
        String[] words = G.splitSpace(strLine);//strLine.split("\\s+");

        // Public
        //
        if (words[0].equalsIgnoreCase("public")) {
            functionScope = "public";
            functionName = words[2];
            m_vbFunctionName = words[2];
            if (words[1].equalsIgnoreCase("sub")) {
                functionType = "void";
            }
            else if (words[1].equalsIgnoreCase("function")) {
                functionType = getFunctionType(strLine);
            }
            // properties
            //
            else {
                if (functionName.equalsIgnoreCase("get")) {
                    functionName = "get" 
                                    + words[3].substring(0,1).toUpperCase()
                                    + words[3].substring(1);
                    m_vbFunctionName = words[3];
                    functionType = getPropertyType(strLine);
                }
                else {
                    functionName = "set" 
                                    + words[3].substring(0,1).toUpperCase()
                                    + words[3].substring(1);
                    m_vbFunctionName = words[3];
                    functionType = "void";
                }
            }
        }
        // Private
        //
        else if (words[0].equalsIgnoreCase("private")) {
            functionScope = "private";
            functionName = words[2];
            m_vbFunctionName = words[2];
            if (words[1].equalsIgnoreCase("sub")) {
                functionType = "void";
            }
            else if (words[1].equalsIgnoreCase("function")) {
                functionType = getFunctionType(strLine);
            }
            // properties
            //
            else {
                if (functionName.equalsIgnoreCase("get")) {
                    functionName = "get" 
                                    + words[3].substring(0,1).toUpperCase()
                                    + words[3].substring(1);
                    m_vbFunctionName = words[3];
                    functionType = getPropertyType(strLine);
                }
                else {
                    functionName = "set" 
                                    + words[3].substring(0,1).toUpperCase()
                                    + words[3].substring(1);
                    m_vbFunctionName = words[3];
                    functionType = "void";
                }
            }
        }
        // Friend
        //
        else if (words[0].equalsIgnoreCase("friend")) {
            functionScope = "public";
            functionName = words[2];
            m_vbFunctionName = words[2];
            if (words[1].equalsIgnoreCase("sub")) {
                functionType = "void";
            }
            else if (words[1].equalsIgnoreCase("function")) {
                functionType = getFunctionType(strLine);
            }
            // properties
            //
            else {
                if (functionName.equalsIgnoreCase("get")) {
                    functionName = "get" 
                                    + words[3].substring(0,1).toUpperCase()
                                    + words[3].substring(1);
                    m_vbFunctionName = words[3];
                    functionType = getPropertyType(strLine);
                }
                else {
                    functionName = "set" 
                                    + words[3].substring(0,1).toUpperCase()
                                    + words[3].substring(1);
                    m_vbFunctionName = words[3];
                    functionType = "void";
                }
            }
        }
        else if (words[0].equalsIgnoreCase("function")) {
            functionScope = "private";
            functionName = words[1];
            m_vbFunctionName = words[1];
            functionType = getFunctionType(strLine);
        }
        else if (words[0].equalsIgnoreCase("sub")) {
            functionScope = "private";
            functionName = words[1];
            m_vbFunctionName = words[1];
            functionType = "void";
        }
        else if (words[0].equalsIgnoreCase("property")) {
            functionScope = "private";
            functionName = words[1];
            m_vbFunctionName = words[1];
            if (functionName.equalsIgnoreCase("get")) {
                functionName = "get" 
                                + words[2].substring(0,1).toUpperCase()
                                + words[2].substring(1);
                m_vbFunctionName = words[2];
                functionType = getPropertyType(strLine);
            }
            else {
                functionName = "set" 
                                + words[2].substring(0,0).toUpperCase()
                                + words[2].substring(1);
                m_vbFunctionName = words[2];
                functionType = "void";
            }
        }

        if (functionName.contains("(")) {
            functionName = functionName.substring(0,functionName.indexOf("("));
            m_vbFunctionName = m_vbFunctionName.substring(0,m_vbFunctionName.indexOf("("));
        }

        if (functionName.length() < 1) {
            functionName = "";
            m_vbFunctionName = "";
        }

        if (!functionName.isEmpty() && functionScope.equals("public"))
            saveFunction(m_vbFunctionName, functionName, functionType);

        m_returnValue = getDefaultForReturnType(functionType);

        return functionScope + " "
                + functionType + " "
                + functionName.substring(0,1).toLowerCase()
                + functionName.substring(1) + "("
                + translateParameters(strLine)
                + ") {"
                + newline;
    }

    private String getPropertyType(String strLine) {
        return getFunctionType(strLine);
    }

    private String getFunctionType(String strLine) {
        int endParams = getEndParams(strLine);
        if (endParams < 0) {
            return "";
        }
        else {
            if (strLine.length() >= endParams + 2) {
                strLine = strLine.substring(endParams + 2);
                String[] words = G.splitSpace(strLine);//strLine.split("\\s+");
                if (words.length >=2) {
                    return getDataType(words[1]);
                }
                else
                    return "Object";
            }
            else
                return "Object";
        }
    }

    private String translateParameters(String strLine) {
        int startParams = getStartParams(strLine);
        int endParams = getEndParams(strLine);

        if (endParams - startParams > 0) {
            String params = strLine.substring(startParams + 1, endParams);
            String[] words = G.split3(params, ",");
            params = "";
            for (int i = 0; i < words.length; i++) {
                params += getParam(words[i]) + ", ";
            }
            if (params.isEmpty())
                return params;
            else
                return params.substring(0,params.length()-2);
        }
        else
            return "";
    }

    private String getParam(String strParam) {
        String paramName = "";
        String vbParamName = "";
        String dataType = "";
        String[] words = G.splitSpace(strParam);//strParam.split("\\s+");

        // empty string
        //
        if (words.length == 0) {
            return "";
        }
        // param_name
        //
        else if (words.length == 1) {
            vbParamName = words[0];
            paramName = getParamName(words[0]);
        }
        // byval param_name
        // byref param_name
        // optional param_name
        //
        else if (words.length == 2) {
            // byval param_name
            if (words[0].equalsIgnoreCase("ByVal")) {
                vbParamName = words[1];
                paramName = getParamName(words[1]);
            }
            // byref param_name
            else if (words[0].equalsIgnoreCase("ByRef")) {
                vbParamName = words[1];
                paramName = getParamName(words[1]);
            }
            // optional param_name
            else if (words[0].equalsIgnoreCase("Optional")) {
                vbParamName = words[1];
                paramName = getParamName(words[1]);
            }
            else
                return "[*TODO: param declaration unsuported -> "
                        + words[0]+ " " + words[1] + "]";
        }
        // param_name As param_type
        // optional byval param_name
        // optional byref param_name
        //
        else if (words.length == 3) {
            // param_name As param_type
            if (words[1].equalsIgnoreCase("as")) {
                dataType = getDataType(words[2]);
                vbParamName = words[0];
                paramName = getParamName(words[0]);
            }
            // optional byval param_name
            // optional byref param_name
            else {
                vbParamName = words[2];
                paramName = getParamName(words[2]);
            }
        }
        // byval param_name As param_type
        // byref param_name As param_type
        // optional param_name As param_type
        // optional param_name = default_value
        //
        else if (words.length == 4) {
            // byval param_name As param_type
            // byref param_name As param_type
            // optional param_name As param_type
            if (words[2].equalsIgnoreCase("as")) {
                dataType = getDataType(words[3]);
                vbParamName = words[1];
                paramName = getParamName(words[1]);
            }
            // optional param_name = default_value
            else if (words[2].equalsIgnoreCase("=")) {
                vbParamName = words[1];
                paramName = getParamName(words[1]);
            }
            else
                return "[*TODO: param declaration unsuported -> "
                        + words[0]+ " " + words[1]
                        + words[2]+ " " + words[3] + "]";
        }
        // byval optional param_name As data_type
        // byref optional param_name As data_type
        // byval optional param_name = default_value
        // byref optional param_name = default_value
        //
        else if (words.length == 5) {
            // byval optional param_name As data_type
            // byref optional param_name As data_type
            if (words[3].equalsIgnoreCase("as")) {
                dataType = getDataType(words[4]);
                vbParamName = words[2];
                paramName = getParamName(words[2]);
            }
            // byval optional param_name = default_value
            // byref optional param_name = default_value
            else if (words[3].equalsIgnoreCase("=")) {
                vbParamName = words[2];
                paramName = getParamName(words[2]);
            }
            else
                return "[*TODO: param declaration unsuported -> "
                        + words[0]+ " " + words[1]
                        + words[2]+ " " + words[3]
                        + words[4]+ "]";
        }
        // optional param_name As data_type = default_value
        //
        else if (words.length == 6) {
            // byval optional param_name As data_type
            // byref optional param_name As data_type
            if (words[2].equalsIgnoreCase("as")) {
                dataType = getDataType(words[3]);
                vbParamName = words[1];
                paramName = getParamName(words[1]);
            }
            else
                return "[*TODO: param declaration unsuported -> "
                        + words[0]+ " " + words[1] 
                        + words[2]+ " " + words[3]
                        + words[4]+ " " + words[5] + "]";
        }
        // optional byval param_name As data_type = default_value
        // optional byref param_name As data_type = default_value
        //
        else if (words.length == 7) {
            if (words[3].equalsIgnoreCase("as")) {
                dataType = getDataType(words[4]);
                vbParamName = words[2];
                paramName = getParamName(words[2]);
            }
            else
                return "[*TODO: param declaration unsuported -> "
                        + words[0]+ " " + words[1]
                        + words[2]+ " " + words[3]
                        + words[4]+ " " + words[5]
                        + words[6]+ "]";
        }
        else
            return "[*TODO: param declaration unsuported -> "
                    + words[0]+ " " + words[1]
                    + words[2]+ " " + words[3]
                    + words[4]+ " " + words[5] + "]";

        if (G.endLike(paramName,"()")) {
            dataType += "[]";
            paramName = paramName.substring(0, paramName.length()-2);
        }
        if (G.endLike(vbParamName, "()")) {
            vbParamName = vbParamName.substring(0, vbParamName.length()-2);
        }

        Variable var = new Variable();
        var.setJavaName(paramName);
        var.setVbName(vbParamName);
        var.setType(dataType);
        m_functionVariables.add(var);

        saveParam(vbParamName, paramName, dataType);

        return dataType + " " + paramName;
    }

    private String getParamName(String paramName) {
        return unCapitalize(paramName);
    }

    private String getVariableName(String paramName) {
        return unCapitalize(paramName);
    }

    private String unCapitalize(String word) {
        if (word.length() > 0) {
            if (word.length() > 1) {
                word = word.substring(0,1).toLowerCase() + word.substring(1);
            }
            else {
                word = word.toLowerCase();
            }
        }
        return word;
    }

    private String getDataType(String dataType) {
        if (dataType.equalsIgnoreCase("byte")) {
            dataType = "byte";
        }
        else if (dataType.equalsIgnoreCase("boolean")) {
            dataType = "boolean";
        }
        else if (dataType.equalsIgnoreCase("double")) {
            dataType = "double";
        }
        else if (dataType.equalsIgnoreCase("integer")) {
            dataType = "int";
        }
        else if (dataType.equalsIgnoreCase("long")) {
            dataType = "long";
        }
        else if (dataType.equalsIgnoreCase("single")) {
            dataType = "float";
        }
        else if (dataType.equalsIgnoreCase("date")) {
            dataType = "Date";
        }
        else if (dataType.equalsIgnoreCase("string")) {
            dataType = "String";
        }
        else if (dataType.equalsIgnoreCase("variant")) {
            dataType = "Object";
        }
        // else: if is not one of the above list we return
        // the same value we received
        //
        return dataType;
    }

    private String getDefaultForReturnType(String dataType) {
        if (dataType.equalsIgnoreCase("byte")) {
            return "0";
        }
        else if (dataType.equalsIgnoreCase("boolean")) {
            return "false";
        }
        else if (dataType.equalsIgnoreCase("double")) {
            return "0";
        }
        else if (dataType.equalsIgnoreCase("int")) {
            return "0";
        }
        else if (dataType.equalsIgnoreCase("long")) {
            return "0";
        }
        else if (dataType.equalsIgnoreCase("float")) {
            return "0";
        }
        else if (dataType.equalsIgnoreCase("Date")) {
            return "null";
        }
        else if (dataType.equalsIgnoreCase("String")) {
            return "\"\"";
        }
        else if (dataType.equalsIgnoreCase("Object")) {
            return "null";
        }
        // else: if is not one of the above list we return
        // a null string
        //
        return "";
    }

    private String getZeroValueForDataType(String dataType) {
        if (dataType.equalsIgnoreCase("byte")) {
            return "0";
        }
        else if (dataType.equalsIgnoreCase("boolean")) {
            return "false";
        }
        else if (dataType.equalsIgnoreCase("double")) {
            return "0";
        }
        else if (dataType.equalsIgnoreCase("int")) {
            return "0";
        }
        else if (dataType.equalsIgnoreCase("long")) {
            return "0";
        }
        else if (dataType.equalsIgnoreCase("float")) {
            return "0";
        }
        else if (dataType.equalsIgnoreCase("Date")) {
            return "null";
        }
        else if (dataType.equalsIgnoreCase("String")) {
            return "\"\"";
        }
        else if (dataType.equalsIgnoreCase("Object")) {
            return "null";
        }
        else {
            return "null";
        }
    }

    private int getStartParams(String strLine) {
        boolean literalFlag = false;
        for (int i = 0; i < strLine.length(); i++) {
            if (strLine.charAt(i) == '"') {
                literalFlag = !literalFlag;
            }
            else if (strLine.charAt(i) == '(') {
                if (!literalFlag) {
                    return i;
                }
            }
        }
        return -1;
    }

    private int getEndParams(String strLine) {
        boolean literalFlag = false;
        int openParentheses = 0;
        for (int i = 0; i < strLine.length(); i++) {
            if (strLine.charAt(i) == '"') {
                literalFlag = !literalFlag;
            }
            if (!literalFlag) {
                if (strLine.charAt(i) == '(') {
                    openParentheses++;
                }
                else if (strLine.charAt(i) == ')') {
                    if (openParentheses > 1) {
                        openParentheses--;
                    }
                    else {
                        return i;
                    }
                }
            }
        }
        return -1;
    }

    private String translatePrivateConstMember(String strLine) {
        // form is
            // private const identifier as data_type = value
            // private const identifier = value
        strLine = strLine.trim();

        int startComment = getStartComment(strLine);
        String workLine = strLine;
        String comments = "";
        if (startComment >= 0) {
            comments =  "//" + workLine.substring(startComment);
            workLine = workLine.substring(0, startComment-1);
        }

        String[] words = G.splitSpace(workLine);//workLine.split("\\s+");
        String dataType = "";
        String identifier = "";
        String constValue = "";

        // private const identifier as data_type = value
        //
        if (words.length > 5) {
            identifier = words[2];
            dataType = words[4];
            constValue = words[6];
        }
        // private const identifier = value
        //
        else if (words.length == 5) {
            identifier = words[2];
            constValue = words[4];
        }
        else {
            return "*" + strLine + newline;
        }
        if (dataType.isEmpty()) {
            if (constValue.charAt(0) == '"') {
                dataType = "String";
            }
            else if (constValue.charAt(0) == '#'){
                dataType = "Date";
            }
            else if (C_NUMBERS.contains(String.valueOf(constValue.charAt(0)))){
                dataType = "int";
            }
            else if (constValue.substring(0,2).equalsIgnoreCase("&h")) {
                dataType = "int";
                constValue = "0x" + constValue.substring(2);
            }
            else {
                return "*TODO: (the data type can't be found for the value ["
                        + constValue + "])" + strLine + newline;
            }
        }

        String vbIdentifier = identifier;
        identifier = identifier.toUpperCase();

        saveVariable(vbIdentifier, identifier, dataType);

        return "private static final "
                + dataType + " "
                + identifier + " = "
                + constValue + ";"
                + comments + newline;
    }

    private void saveFunction(String vbIdentifier, String identifier, String dataType) {
        m_functionObject.setClId(m_classObject.getId());
        m_functionObject.setVbName(vbIdentifier);
        m_functionObject.setJavaName(identifier);
        m_functionObject.setDataType(dataType);
        m_functionObject.getFunctionIdFromFunctionName();
        m_functionObject.saveFunction();
    }

    private void saveParam(String vbParamName, String paramName, String dataType) {
        saveVariable(vbParamName, paramName, dataType, true, false);
    }
    
    private void saveVariable(String vbIdentifier, String identifier, String dataType) {
        saveVariable(vbIdentifier, identifier, dataType, false, false);
    }

    private void saveVariable(String vbIdentifier,
                                String identifier,
                                String dataType,
                                boolean isParameter,
                                boolean isPublic) {

        m_variableObject.setClId(m_classObject.getId());
        m_variableObject.setVbName(vbIdentifier);
        m_variableObject.setJavaName(identifier);
        m_variableObject.setFunId(m_functionObject.getId());
        m_variableObject.setDataType(dataType);
        m_variableObject.setIsParameter(isParameter);
        m_variableObject.setIsPublic(isPublic);
        m_variableObject.getVariableIdFromVariableName();
        m_variableObject.saveVariable();
    }

    private void saveVariableInType(String vbIdentifier, String identifier, String dataType) {
        m_variableObject.setClId(m_typeClassObject.getId());
        m_variableObject.setVbName(vbIdentifier);
        m_variableObject.setJavaName(identifier);
        m_variableObject.setFunId(Db.CS_NO_ID);
        m_variableObject.setDataType(dataType);
        m_variableObject.setIsParameter(false);
        m_variableObject.setIsPublic(true);
        m_variableObject.getVariableIdFromVariableName();
        m_variableObject.saveVariable();
    }

    private String translatePublicConstMember(String strLine) {
        // form is
            // dim variable_name as data_type
        strLine = strLine.trim();
        String[] words = G.splitSpace(strLine);//strLine.split("\\s+");
        String dataType = "";
        String identifier = "";
        String constValue = "";
        String misc = "";

        if (words.length > 2) {
            identifier = words[2];
            if (words.length >= 4) {
                if (words[4].equals("=")) {
                    if (words.length > 4) {
                        dataType = words[4];
                    }
                    if (words.length >= 5) {
                        constValue = words[5];
                    }
                    for (int i = 6; i < words.length; i++) {
                        misc += " " + words[i] ;
                    }
                }
                else {
                    if (words.length >= 4) {
                        constValue = words[4];
                    }
                    for (int i = 5; i < words.length; i++) {
                        misc += " " + words[i] ;
                    }
                }
            }
            else {
                return "*" + strLine + newline;
            }
        }
        else {
            return "*" + strLine + newline;
        }
        if (dataType.isEmpty()) {
            if (constValue.charAt(0) == '"') {
                dataType = "String";
            }
            else if (constValue.charAt(0) == '#'){
                dataType = "Date";
            }
            else if (C_NUMBERS.contains(String.valueOf(constValue.charAt(0)))){
                dataType = "int";
            }
            else if (constValue.substring(0,2).equalsIgnoreCase("&h")) {
                dataType = "int";
                constValue = "0x" + constValue.substring(2);
            }
            else {
                return "*TODO: (the data type can't be found for the value ["
                        + constValue + "])" + strLine + newline;
            }
        }

        String vbIdentifier = identifier;
        identifier = identifier.toUpperCase();

        saveVariable(vbIdentifier, identifier, dataType);

        return "public static final " + dataType + " " + identifier + " = "
                + constValue + ";" + misc + newline;
    }

    private String translatePrivateMember(String strLine) {
        // form is
            // dim variable_name as data_type
        strLine = strLine.trim();
        String[] words = G.splitSpace(strLine);//strLine.split("\\s+");
        String dataType = "";
        String identifier = "";
        String vbIdentifier = "";
        String misc = "";
        boolean isEventGenerator = false;

        if (words.length > 1) {
            vbIdentifier = words[1];

            // with events eg:
            //      private withevents my_obj_with_events as CObjetWithEvents ' some comments
            //      0       1           2                 3        4          >= 5
            //
            if (vbIdentifier.equalsIgnoreCase("WithEvents")) {
                vbIdentifier = words[2];
                identifier = getIdentifier(vbIdentifier);
                if (words.length > 4) {
                    dataType = words[4];
                }
                for (int i = 5; i < words.length; i++) {
                    misc += " " + words[i] ;
                }
                addToEventListeners(vbIdentifier, dataType);
                isEventGenerator = true;
            }
            else {
                identifier = getIdentifier(vbIdentifier);
                if (words.length > 3) {
                    dataType = words[3];
                }
                for (int i = 4; i < words.length; i++) {
                    misc += " " + words[i] ;
                }
            }
        }
        if (!identifier.isEmpty()) {
            Variable var = new Variable();
            var.setVbName(vbIdentifier);
            var.setJavaName(identifier);
            var.setType(dataType);
            var.isEventGenerator = isEventGenerator;
            m_memberVariables.add(var);
        }
        if (dataType.isEmpty()) {
            dataType = "Object";
        }
        dataType = getDataType(dataType);

        saveVariable(vbIdentifier, identifier, dataType);

        return "private " + dataType + " " + identifier + ";" + misc + newline;
    }

    private String translatePublicMember(String strLine) {
        // form is
            // dim variable_name as data_type
        strLine = strLine.trim();
        String[] words = G.splitSpace(strLine);//strLine.split("\\s+");
        String dataType = "";
        String identifier = "";
        String vbIdentifier = "";
        String misc = "";
        boolean isEventGenerator = false;

        if (words.length > 1) {
            vbIdentifier = words[1];
            // with events eg:
            //      private withevents my_obj_with_events as CObjetWithEvents ' some comments
            //      0       1           2                 3        4          >= 5
            //
            if (vbIdentifier.equalsIgnoreCase("WithEvents")) {
                vbIdentifier = words[2];
                identifier = getIdentifier(vbIdentifier);
                if (words.length > 4) {
                    dataType = words[4];
                }
                for (int i = 5; i < words.length; i++) {
                    misc += " " + words[i] ;
                }
                addToEventListeners(vbIdentifier, dataType);
                isEventGenerator = true;
            }
            else {
                identifier = getIdentifier(vbIdentifier);
                if (words.length > 3) {
                    dataType = words[3];
                }
                for (int i = 4; i < words.length; i++) {
                    misc += " " + words[i] ;
                }
            }
        }
        if (!identifier.isEmpty()) {
            Variable var = new Variable();
            var.setVbName(vbIdentifier);
            var.setJavaName(identifier);
            var.setType(dataType);
            var.isEventGenerator = isEventGenerator;
            m_memberVariables.add(var);
        }
        if (dataType.isEmpty()) {
            dataType = "Object";
        }
        dataType = getDataType(dataType);

        saveVariable(vbIdentifier, identifier, dataType, false, true);

        Variable var = new Variable();
        var.setVbName(vbIdentifier);
        var.setJavaName(identifier);
        var.packageName = m_packageName;
        var.setType(dataType);
        var.isPublic = true;
        m_publicVariables.add(var);

        return "public " + dataType + " " + identifier + ";" + misc + newline;
    }

    private String getIdentifier(String word) {
        String identifier = "";
        if (word.length() > 2 ) {
            if (word.substring(0,2).equals("m_")) {
                identifier = word.substring(0,3).toLowerCase();
                if (word.length() > 3 ) {
                    identifier += word.substring(3);
                }
            }
            else{
                identifier = word;
            }
        }
        else {
            identifier = word;
        }
        return identifier;
    }

    // we need three elements in custom events
    //  -- event class
    //  -- event listener interface
    //  -- event generator
    //
    // a) when the class which we are translating has public events
    // (private events doesn't have sense) we have to create the
    // event listener interface with a method for every public event and
    // a class which implements the event listener interface as and adapter
    // class (to free the listener to implement all the methods of the interface).
    // the listeners will extend the adapter class as an inner anonymous class.
    // the interface name will be named as the class plus the
    // postfix EventI eg: for a class named in vb6 code as cReport the interface
    // will be CReportEventI (remeber that every class will be capitalized)
    // and the adapter will be CReportEventA
    //
    // b) when the class which we are translating is the event listener
    // it has to declare an anonymous inner classes which extend
    // the adapter class (which implement the event listener
    // interface) for every variable which raises events.
    //
    // c) in visual basic 6 you need to instantiate the member variable
    // which generate events with an explicit assignment like
    //
    //      set m_event_generator = new ClassEventGenerator
    // or
    //      set m_event_generator = already_instantiated_event_generator
    //
    // we have to add after that point a call to the addListener method of the
    // event generator object.
    //
    // the problem is that we are translating in one read of the content
    // line by line from up to down and so at the point of this asignment
    // line we can't be sure that we know every event our class is
    // intrested to listen to. for this reason we need to reach the end
    // of the file to be sure we know all the code related to events of 
    // a "with events variable".
    //
    // to fix it we will add a macro to be replace after translating the class
    // with the definition of the anonymous inner class for every "with events
    // variable". this macro will be:
    //          __ADD_TO_LISTENER_name_of_the_generator_variable__
    //
    //  eg: if the generetor is m_report the macro will be
    //
    //          __ADD_TO_LISTENER_m_report__
    //
    private void addToEventListeners(String eventGenerator, String className) {
        EventListener eventListener = new EventListener();
        eventListener.setGenerator(eventGenerator);
        eventListener.setAdapter(className);
        m_eventListeners.add(eventListener);
    }

    private String getEventMacroName(String variable) {
        return "__ADD_TO_LISTENER_" + variable + "__";
    }

    private String translateDim(String strLine) {
        // form is
            // dim variable_name as data_type
        strLine = strLine.trim();
        String[] words = G.splitSpace(strLine);//strLine.split("\\s+");
        String dataType = "";
        String vbIdentifier = "";
        String identifier = "";
        String misc = "";

        if (words.length > 1) {
            vbIdentifier = words[1];
            identifier = getVariableName(words[1]);
            if (words.length > 3) {
                dataType = words[3];
            }
            for (int i = 4; i < words.length; i++) {
                misc += " " + words[i] ;
            }
        }
        if (dataType.isEmpty()) {
            dataType = "Object";
        }
        dataType = getDataType(dataType);

        Variable var = new Variable();
        var.setJavaName(identifier);
        var.setVbName(vbIdentifier);
        var.setType(dataType);
        m_functionVariables.add(var);

        return dataType + " " + identifier + " = "
                + getZeroValueForDataType(dataType) + ";" + misc + newline;
    }

    private String removeExtraSpaces(String strLine) {
        String rtn = "";
        boolean previousWasAChar = false;
        for (int i = 0; i < strLine.length(); i++) {
            if (strLine.charAt(i) == ' ') {
                if (!previousWasAChar) {
                    rtn += " ";
                    previousWasAChar = true;
                }
            }
            else {
                rtn += strLine.charAt(i);
                previousWasAChar = false;
            }
        }
        return rtn;
    }

    public void initDbObjects() {
        m_classObject = new ClassObject();
        m_functionObject = new FunctionObject();
        m_variableObject = new VariableObject();
        m_typeClassObject = new ClassObject();
    }

    public void initTranslator(String name) {
        m_isVbSource = false;
        m_codeHasStarted = false;
        m_attributeBlockHasStarted = false;
        m_inFunction = false;
        m_inEnum = false;
        m_inWith = false;
        m_withDeclaration = false;
        m_endWithDeclaration = false;
        m_type = "";
        m_vbClassName = "";
        m_javaClassName = "";
        m_collTypes.removeAll(m_collTypes);
        m_collEnums.removeAll(m_collEnums);
        m_collWiths.removeAll(m_collWiths);
        m_eventListeners.removeAll(m_eventListeners);
        m_memberVariables.removeAll(m_memberVariables);
        m_functionVariables.removeAll(m_functionVariables);
        m_privateFunctions = new ArrayList<Function>();
        m_publicFunctions = new ArrayList<Function>();
        m_publicVariables = new ArrayList<Variable>();
        m_types = new ArrayList<Type>();
        m_tabCount = 0;
        m_addDateAuxFunction = false;
        m_returnValue = "";

        if (name.contains(".")) {
            if (name.length() > 0) {
                String ext = name.substring(name.length()-3).toLowerCase();
                if ( ext.equals("bas") || ext.equals("cls") || ext.equals("frm") ) {
                    m_isVbSource = true;
                }
            }
        }
    }

    private void saveTypeClassInDB(String className) {
        int i = className.indexOf("'");
        if (i > 0) {
            className = className.substring(0, i - 1).trim();
        }
        i = className.indexOf(" ");
        if (i > 0) {
            className = className.substring(0, i - 1).trim();
        }        
        m_typeClassObject.setPackageName(m_packageName);
        m_typeClassObject.setVbName(className);
        m_typeClassObject.setJavaName(className);
        m_typeClassObject.getClassIdFromClassName();
        m_typeClassObject.saveClass();
    }

    private void addToType(String strLine) {
        String className = "";
        strLine = G.ltrim(strLine);

        if (strLine.length() > 5) {
            if (strLine.substring(0,5).toLowerCase().equals("type ")) {
                Type type = new Type();
                type.vbName = strLine.substring(5);
                className = type.vbName;
                type.javaName = className;
                type.getVbCode().append(strLine);
                m_types.add(type);
                m_type += "private class " + className + " {" + newline;
                saveTypeClassInDB(className);
                return;
            }
        }

        if (strLine.length() > 12) {
            if (strLine.substring(0,12).toLowerCase().equals("public type ")) {
                Type type = new Type();
                type.isPublic = true;
                type.vbName = strLine.substring(12);
                className = type.vbName;
                type.javaName = className;
                type.getVbCode().append(strLine);
                m_types.add(type);
                m_type += "public class " + className + " {" + newline;
                saveTypeClassInDB(className);
                return;
            }
        }

        if (strLine.length() > 13) {
            if (strLine.substring(0,13).toLowerCase().equals("private type ")) {
                Type type = new Type();
                type.vbName = strLine.substring(13);
                className = type.vbName;
                type.javaName = className;
                type.getVbCode().append(strLine);
                m_types.add(type);
                m_type += "private class " + className + " {" + newline;
                saveTypeClassInDB(className);
                return;
            }
        }

        if (strLine.trim().length() == 8) {
            if (strLine.substring(0,8).toLowerCase().equals("end type")) {
                m_inType = false;
                m_type += "}" + newline + newline;
                m_collTypes.add(m_type);
                Type type = m_types.get(m_types.size()-1);
                type.getVbCode().append(strLine);
                type.getJavaCode().append(m_type);
                m_type = "";
                if (type.isPublic)
                    m_caller.addPublicType(type);
            }
        }
        else {
            String dataType = "";
            String identifier = "";

            Type type = m_types.get(m_types.size()-1);
            type.getVbCode().append(strLine);

            strLine = strLine.trim();
            String[] words = G.splitSpace(strLine);//strLine.split("\\s+");

            if (words.length > 0) {

                // check to see if it is an array
                //
                if (words[0].contains("(")) {

                    String size = words[0].substring(words[0].indexOf("(")+1);
                    if (size.equals(")")) {
                        dataType = "Object[]";
                    }
                    else {
                        if (words.length >= 3) {
                            if (words[1].equalsIgnoreCase("to")) {

                                int lowBound = Integer.parseInt(size);
                                size = words[2].substring(0,words[2].length()-1);
                                String upperBound = size;
                                if (lowBound == 1) {
                                    size = upperBound;
                                }
                                else {
                                    size = upperBound + " - " + ((Integer)(lowBound-1)).toString();
                                }

                                // complete sentence eg: type_member(low_bound to upper_bound) as data_type
                                //                       |                   | |  |          | |  |
                                //                       1                     2  3            4  5
                                //
                                if (words.length >= 5) {
                                    if (words[4].charAt(0) =='\'') {
                                        dataType = "Object[" + size + "]";
                                    }
                                    else {
                                        dataType = words[4] + "[" + size + "]";
                                    }
                                }
                                else {
                                    dataType = "Object[" + size + "]";
                                }
                            }
                            else {
                                size = words[0].substring(words[0].indexOf("(")+1);
                                size = size.substring(0, size.length()-1);
                                dataType = words[2] + "[" + size + "]";
                            }
                        }
                        // variant array like: type_member(dimension)
                        //
                        else {
                            size = words[0].substring(words[0].indexOf("(")+1);
                            size = size.substring(0, size.length()-1);
                            dataType = words[2] + "[" + size + "]";
                        }
                    }
                    identifier = words[0].substring(0, words[0].indexOf("("));
                }
                else {
                    // complete sentence eg: type_member as data_type
                    //
                    if (words.length >= 3) {

                        if (words[1].charAt(0) =='\'') {
                            dataType = "Object";
                        }
                        else {
                            if (words[2].charAt(0) =='\'') {
                                dataType = "Object";
                            }
                            else {
                                dataType = words[2];
                            }
                        }
                    }
                    // implicit sentence eg: type_member {no declaration of type}
                    //
                    else {
                        dataType = "Object";
                    }
                    identifier = words[0];
                }

                saveVariableInType(identifier, identifier, dataType);

                Variable var = new Variable();
                var.setVbName(identifier);
                var.setJavaName(identifier);
                var.setType(dataType);
                var.isPublic = true;
                type.getMembersVariables().add(var);

                m_type += "    public " + dataType + ' ' + identifier + ";" + newline;
            } 
            else {
                m_type += strLine;
            }
        }
    }

    private void addToEnum(String strLine) {
        strLine = G.ltrim(strLine);

        if (strLine.length() > 5) {
            if (strLine.substring(0, 5).toLowerCase().equals("enum ")) {
                m_enum += "private class " + strLine.substring(5) + " {" + newline;
                return;
            }
        }

        if (strLine.length() > 12) {
            if (strLine.substring(0, 12).toLowerCase().equals("public enum ")) {
                m_enum += "public class " + strLine.substring(12) + " {" + newline;
                return;
            }
        }

        if (strLine.length() > 13) {
            if (strLine.substring(0, 13).toLowerCase().equals("private enum ")) {
                m_enum += "private class " + strLine.substring(13) + " {" + newline;
                return;
            }
        }

        if (strLine.trim().length() == 8) {
            if (strLine.substring(0, 8).toLowerCase().equals("end enum")) {
                m_inEnum = false;
                int lastColon = 0;
                for (int i = 0; i < m_enum.length(); i++) {
                    if (m_enum.charAt(i) == ',') {
                        lastColon = i;
                    }
                    else if (m_enum.charAt(i) == '\'') {
                        break;
                    }
                }
                if (lastColon > 0) {
                    m_enum = m_enum.substring(0,lastColon) + m_enum.substring(lastColon+1);
                }
                m_enum += "}" + newline + newline;
                m_collEnums.add(m_enum);
                m_enum = "";
            }
        }
        else {
            String constValue = "";
            String identifier = "";
            String misc = "";

            strLine = strLine.trim();
            String[] words = G.splitSpace(strLine);//strLine.split("\\s+");

            if (words.length > 0) {

                identifier = words[0];
                
                // complete sentence eg: enum_member = enum_value
                //
                if (words.length >= 3) {

                    if (words[1].charAt(0) == '=') {
                        constValue = words[2];
                    }
                }
                // implicit sentence eg: enum_member {no declaration of value}
                //
                else {
                    int lenIdentifier = identifier.length();
                    if (strLine.length() > lenIdentifier) {
                        misc = "//" + strLine.substring(lenIdentifier);
                    }
                }

                if (constValue.isEmpty())
                    m_enum += "    " + identifier.toUpperCase() + "," + misc + newline;
                else
                    if (constValue.length() > 2) {
                        if (constValue.substring(0, 2).equalsIgnoreCase("&h")) {
                            constValue = "0x" + constValue.substring(2);
                        }
                    }
                    m_enum += "    " + identifier.toUpperCase() + " = "
                            + constValue + "," + misc + newline;
            }
            else {
                m_enum += strLine;
            }
        }
    }

    public String getSubClasses() {
        String subClasses = "";
        for (int i = 0; i < m_collTypes.size(); i++) {
            subClasses += m_collTypes.get(i) + newline;
        }
        for (int i = 0; i < m_collEnums.size(); i++) {
            subClasses += m_collEnums.get(i) + newline;
        }
        return subClasses;
    }

    private void checkBeginBlock(String strLine) {
        m_wasSingleLineIf = false;
        strLine = G.ltrimTab(strLine);

        // If
        //
        if (G.beginLike(strLine, "If ")) {
            int startComment = getStartComment(strLine);
            if (startComment >= 0) {
                strLine = strLine.substring(0, startComment-1);
            }
            strLine = strLine.trim();
            if (G.endLike(strLine, " Then")) {
                m_tabCount++;
                m_wasSingleLineIf = false;
            }
            else {
                m_wasSingleLineIf = true;
            }
        }
        // Else If
        //
        else if (G.beginLike(strLine, "ElseIf ")) {
            int startComment = getStartComment(strLine);
            if (startComment >= 0) {
                strLine = strLine.substring(0, startComment-1);
            }
            strLine = strLine.trim();
            if (G.endLike(strLine, " Then")) {
                m_tabCount++;
                m_wasSingleLineIf = false;
            }
            else {
                m_wasSingleLineIf = true;
            }
        }
        // Else
        //
        else if (G.beginLike(strLine, "Else ")) {
            int startComment = getStartComment(strLine);
            if (startComment >= 0) {
                strLine = strLine.substring(0, startComment-1);
            }
            strLine = strLine.trim();
            if (strLine.trim().equalsIgnoreCase("Else")) {
                m_tabCount++;
                m_wasSingleLineIf = false;
            }
            else {
                m_wasSingleLineIf = true;
            }
        }
        // Else
        //
        else if (strLine.trim().equalsIgnoreCase("Else")) {
            m_tabCount++;
        }
        // For
        //
        else if (G.beginLike(strLine, "For ")) {
            m_tabCount++;
        }
        // While
        //
        else if (G.beginLike(strLine, "While ")) {
            m_tabCount++;
        }
        // Do
        //
        else if (G.beginLike(strLine, "Do ")) {
            m_tabCount++;
        }
        // With
        //
        else if (G.beginLike(strLine, "With ")) {
            m_tabCount++;
        }
        // Select Case
        //
        else if (G.beginLike(strLine, "Select Case ")) {
            m_tabCount+=2;
        }
        // Case
        //
        else if (G.beginLike(strLine, "Case ")) {
            m_tabCount++;
        }
        // Public Function
        //
        else if (G.beginLike(strLine, "Public Function ")) {
            m_tabCount++;
        }
        // Private Function
        //
        else if (G.beginLike(strLine, "Private Function ")) {
            m_tabCount++;
        }
        // Public Sub
        //
        else if (G.beginLike(strLine, "Public Sub ")) {
            m_tabCount++;
        }
        // Private Sub
        //
        else if (G.beginLike(strLine, "Private Sub ")) {
            m_tabCount++;
        }
        // Function
        //
        else if (G.beginLike(strLine, "Function ")) {
            m_tabCount++;
        }
        // Sub
        //
        else if (G.beginLike(strLine, "Sub ")) {
            m_tabCount++;
        }
        // Public Property
        //
        else if (G.beginLike(strLine, "Public Property ")) {
            m_tabCount++;
        }
        // Private Property
        //
        else if (G.beginLike(strLine, "Private Property ")) {
            m_tabCount++;
        }
        // Property
        //
        else if (G.beginLike(strLine, "Property ")) {
            m_tabCount++;
        }
        // Friend Function
        //
        else if (G.beginLike(strLine, "Friend Function ")) {
            m_tabCount++;
        }
        // Friend Sub
        //
        else if (G.beginLike(strLine, "Friend Sub ")) {
            m_tabCount++;
        }
        // Friend Property
        //
        else if (G.beginLike(strLine, "Friend Property ")) {
            m_tabCount++;
        }
    }

    private void checkEndBlock(String strLine) {
        strLine = G.ltrimTab(strLine);
        
        int startComment = getStartComment(strLine);
        if (startComment >= 0) {
            strLine = strLine.substring(0, startComment-1);
        }
        
        if (strLine.trim().equalsIgnoreCase("End If")) {
            m_tabCount--;
        }
        // ElseIf
        //
        else if (G.beginLike(strLine, "ElseIf ")) {
            //if (G.endLike(strLine, " Then")) {
            //    m_tabCount--;
            //}
            //if (!m_wasSingleLineIf) m_tabCount--;
            m_tabCount--;
        }
        // Else
        //
        else if (G.beginLike(strLine, "Else ")) {
            //if (!m_wasSingleLineIf) m_tabCount--;
            m_tabCount--;
        }
        // Else
        //
        else if (strLine.trim().equalsIgnoreCase("Else")) {
            //if (!m_wasSingleLineIf) m_tabCount--;
            m_tabCount--;
        }
        // End Select
        //
        else if (strLine.trim().equalsIgnoreCase("End Select")) {
            m_tabCount-=2;
        }
        // Case
        //
        else if (G.beginLike(strLine, "Case ")) {
            m_tabCount--;
        }
        // End With
        //
        else if (strLine.trim().equalsIgnoreCase("End With")) {
            m_tabCount--;
        }
        // Loop
        //
        else if (G.beginLike(strLine, "Loop ")) {
            m_tabCount--;
        }
        else if (strLine.trim().equalsIgnoreCase("Loop")) {
            m_tabCount--;
        }
        // Wend
        //
        else if (strLine.trim().equalsIgnoreCase("Wend")) {
            m_tabCount--;
        }
        // Next
        //
        else if (G.beginLike(strLine, "Next ")) {
            m_tabCount--;
        }
        // Next
        //
        else if (strLine.trim().equalsIgnoreCase("Next")) {
            m_tabCount--;
        }
        // End Function
        //
        else if (strLine.trim().equalsIgnoreCase("End Function")) {
            m_tabCount--;
        }
        // End Sub
        //
        else if (strLine.trim().equalsIgnoreCase("End Sub")) {
            m_tabCount--;
        }
        // End Property
        //
        else if (strLine.trim().equalsIgnoreCase("End Property")) {
            m_tabCount--;
        }
        if (m_tabCount < 1) {
            m_tabCount = 1;
        }
    }

    private Boolean isEndFunction(String strLine) {
        strLine = G.ltrimTab(strLine);

        int startComment = getStartComment(strLine);
        if (startComment >= 0) {
            strLine = strLine.substring(0, startComment-1);
        }

        if (strLine.trim().equalsIgnoreCase("End Function")) {
            return true;
        }
        // End Sub
        //
        else if (strLine.trim().equalsIgnoreCase("End Sub")) {
            return true;
        }
        // End Property
        //
        else if (strLine.trim().equalsIgnoreCase("End Property")) {
            return true;
        }
        else
            return false;
    }

    private String getTabs() {
        return G.rep(' ', m_tabCount * 4);
    }

    private String removeLineNumbers(String strLine) {
        boolean isNumber = true;
        strLine = G.ltrimTab(strLine);
        String[] words = G.splitSpace(strLine);//strLine.split("\\s+");

        if (words.length > 0) {
            for (int i = 0; i < words[0].length(); i++) {
                if(!C_NUMBERS.contains(String.valueOf(words[0].charAt(i)))) {
                    isNumber = false;
                    break;
                }
            }
            if (isNumber) {
                return strLine.replaceFirst(words[0], "");
            }
            else {
                return strLine;
            }
        }
        else {
            return strLine;
        }
    }
}

class IdentifierInfo {
    boolean isFunction = false;
    Function function = null;
    Variable variable = null;
}

/*
 And
 As
 Call
 Do
 Exit
 False
 True
 For
 Function
 GoTo
 If
 Loop
 Me
 Next
 Not
 Nothing
 Option
 Or
 Private
 Public
 Resume
 Step
 Sub
 Then
 Until
 While
 If..Else..ElseIf..Then
 */

/*
 * TODO: file mError.bas line 72 {s = Replace(s, "$" & i + 1, X(i))}
 *       the code is translated as
 *              {s = Replace(s, "$" + ((Integer) i).ToString() + 1, X(i));}
 *       it is wrong because i + 1 must to be evaluated first and then has to apply
 *       the cast to Integer:
 *              {s = Replace(s, "$" + ((Integer) (i + 1)).ToString(), X(i));}
 */

/*
 * TODO: manage events
 * TODO: manage byref params that actually aren't byref because are not asigned to a value
 *       by the function code
 * TODO: change getters in assignment eg:
 *              m_obj.getProperty() = ...;
 *       must be
 *              m_obj.setProperty(...);
 * TODO: translate byref for strings
 * TODO: translate byref for arrays. this is for params of array type that are resized
 *       by the code of the function. we have to search for redim
 * TODO: translate redim
 * TODO: translate instr
 * TODO: translate database access. replace recordsets.
 */