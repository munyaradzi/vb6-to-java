/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package buggymastercode;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingWorker;

/**
 *
 * @author jalvarez
 */
public class TranslatorWorker extends SwingWorker<Boolean, Boolean> {

    static private final String newline = "\n";
            
    private String m_path = "";
    private String m_vbpFile = "";
    private ArrayList<SourceFile> m_collFiles = new ArrayList<SourceFile>();
    private Translator translator = new Translator();
    private BuggyMasterCodeView m_caller = null;

    public TranslatorWorker(BuggyMasterCodeView caller, String path, String vbpFile, ArrayList<SourceFile> collFiles) {
        m_path = path;
        m_collFiles = collFiles;
        m_vbpFile = vbpFile;
        m_caller = caller;
    }

    @Override
    protected Boolean doInBackground() throws Exception {
        doWork(m_vbpFile);
        return true;
    }

    @Override
    protected void done() {
        try {
            m_caller.workDone();
        }
        catch (Exception ignore) {
        }
    }

    private void doWork(String vbpFile) {
        ByRefString value = new ByRefString();

        // Parse
        //
        int line = 1;
        if (G.getToken(vbpFile, "Form", line , value)) {
            while (!value.text.isEmpty()) {
                parseFile(value.text);
                line++;
                if (!G.getToken(vbpFile, "Form", line , value)) {
                    break;
                }
                //progressBar.setValue(line);
            }
        }
        line = 1;
        if (G.getToken(vbpFile, "Module", line , value)) {
            while (!value.text.isEmpty()) {
                parseFile(value.text);
                line++;
                if (!G.getToken(vbpFile, "Module", line , value)) {
                    break;
                }
                //progressBar.setValue(line);
            }
        }
        line = 1;
        if (G.getToken(vbpFile, "Class", line , value)) {
            while (!value.text.isEmpty()) {
                parseFile(value.text);
                line++;
                if (!G.getToken(vbpFile, "Class", line , value)) {
                    break;
                }
                //progressBar.setValue(line);
            }
        }

        // Translate
        //
        translator.setSourceFiles(m_collFiles);

        int indexFile = 0;
        line = 1;
        if (G.getToken(vbpFile, "Form", line , value)) {
            while (!value.text.isEmpty()) {
                translateFile(value.text, indexFile++);
                line++;
                if (!G.getToken(vbpFile, "Form", line , value)) {
                    break;
                }
            }
        }
        line = 1;
        if (G.getToken(vbpFile, "Module", line , value)) {
            while (!value.text.isEmpty()) {
                translateFile(value.text, indexFile++);
                line++;
                if (!G.getToken(vbpFile, "Module", line , value)) {
                    break;
                }
            }
        }
        line = 1;
        if (G.getToken(vbpFile, "Class", line , value)) {
            while (!value.text.isEmpty()) {
                translateFile(value.text, indexFile++);
                line++;
                if (!G.getToken(vbpFile, "Class", line , value)) {
                    break;
                }
            }
        }
    }

    public void parseFile(String vbFile) {

        String vbFullFile = vbFile;

        SourceFile sourceFile = new SourceFile();
        m_collFiles.add(sourceFile);

        //txSourceCode.setText("");
        //txSourceCodeJava.setText("");

        FileInputStream fstream = null;

        try {

            if (vbFullFile.contains(";")) {
                vbFullFile = vbFullFile.substring(vbFullFile.indexOf(";")+2);
            }

            vbFullFile = m_path + vbFullFile;

            translator.initTranslator(vbFullFile);

            if (translator.isVbSource()) {
                fstream = new FileInputStream(getFileForOS(vbFullFile));
                DataInputStream in = new DataInputStream(fstream);
                BufferedReader br = new BufferedReader(new InputStreamReader(in));
                String strLine;
                while ((strLine = br.readLine()) != null) {
                    translator.parse(strLine);
                }
                sourceFile.setVbName(translator.getVbClassName());
                sourceFile.setJavaName(translator.getJavaClassName());
                sourceFile.setPublicFunctions(translator.getPublicFunctions());
                sourceFile.setPrivateFunctions(translator.getPrivateFunctions());
                sourceFile.setFileName(vbFile);
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(BuggyMasterCodeView.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(BuggyMasterCodeView.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                fstream.close();
            } catch (IOException ex) {
                Logger.getLogger(BuggyMasterCodeView.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void translateFile(String vbFile, int indexFile) {

        SourceFile sourceFile = m_collFiles.get(indexFile);

        StringBuilder sourceCode = new StringBuilder();
        StringBuilder sourceCodeJava = new StringBuilder();

        FileInputStream fstream = null;

        try {

            if (vbFile.contains(";")) {
                vbFile = vbFile.substring(vbFile.indexOf(";")+2);
            }

            vbFile = m_path + vbFile;

            translator.initTranslator(vbFile);

            fstream = new FileInputStream(getFileForOS(vbFile));
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String strLine;
            while ((strLine = br.readLine()) != null) {
                sourceCode.append(strLine + newline);
                if (translator.isVbSource()) {
                    sourceCodeJava.append(translator.translate(strLine));
                }
            }
            if (translator.isVbSource()) {
                sourceCodeJava.append("}" + newline);
            }
            sourceFile.setVbSource(sourceCode.toString());
            sourceFile.setJavaSource(sourceCodeJava.toString() + newline + translator.getSubClasses());

        } catch (FileNotFoundException ex) {
            Logger.getLogger(BuggyMasterCodeView.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(BuggyMasterCodeView.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                fstream.close();
            } catch (IOException ex) {
                Logger.getLogger(BuggyMasterCodeView.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private String getFileForOS(String file) {
        String nameOS = "os.name";
        if (System.getProperty(nameOS).toLowerCase().contains("windows")) {
            return file;
        }
        else {
            return file.replace("\\", "/");
        }
    }
}
