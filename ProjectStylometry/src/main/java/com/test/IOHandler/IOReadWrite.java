/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.test.IOHandler;

import com.test.model.Alias;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ImageIcon;

/**
 *
 * @author ITE
 */
public class IOReadWrite {
    
    public Alias convertTxtFileToAliasObj(String basePath, String fileName, String extension) throws FileNotFoundException, IOException {
        String userPostAsString = readTxtFileAsString(basePath, fileName, extension);
        String temp[] = null;
        Alias alias = new Alias();
        List<String> postList = new ArrayList<String>();
        List<String> timeList = new ArrayList<String>();
        alias.setUserID(fileName);
        if (userPostAsString.contains(IOProperties.DATA_SEPERATOR)) {
            temp = userPostAsString.split(IOProperties.DATA_SEPERATOR);
        } else {
            temp = new String[1];
            temp[0] = userPostAsString;
        }

        for (int i = 0; i < temp.length; i++) {
            if (temp[i].toString().matches("[0-9]{2}:[0-9]{2}:[0-9]{2}")
                    || temp[i].toString().length() == 8) {
                temp[i] = temp[i].toString() + "  ";
            }
            String date = temp[i].substring(0, 8);
            if (date.matches("[0-9]{2}:[0-9]{2}:[0-9]{2}")) {
                timeList.add(date);
                postList.add(temp[i].substring(9, temp[i].length()));
            } else {
                continue;
            }
        }
        alias.setPostTime(timeList);
        alias.setPosts(postList);
        return alias;
    }
    
    public String readTxtFileAsString(String basePath, String fileName, String extension) throws FileNotFoundException, IOException {
        StringBuilder stringBuilder = new StringBuilder();
        try {
            String filepath = basePath + "/"+ fileName + extension;
            
            File file = new File(filepath);
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line = null;
            if (file.exists()) {
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }
            }
        } catch (FileNotFoundException ex) {
            throw ex;
        } catch (IOException ex) {
            throw ex;
        }
        String a = stringBuilder.substring(0, (stringBuilder.length() - (IOProperties.DATA_SEPERATOR).length())).toString();
        return a;
    }
    
        
    public List<Alias> convertUserToObj(List post1, List post2){
        List<Alias> aliasList = new ArrayList<Alias>();
        Alias alias1 = new Alias();
        Alias alias2 = new Alias();
        
        alias1.setUserID("1");
        alias2.setUserID("2");
        alias1.setPosts(post1);
        alias2.setPosts(post2);
        aliasList.add(alias1);
        aliasList.add(alias2);
        return aliasList;   
    }
    
}
