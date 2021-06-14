package com.caverock.bplist;

public class Main {

    public static void main(String[] args)
    {
        //String  testfile = "C:\\Users\\Paul\\DevelopmentWorkspaces\\IdeaProjects\\BPList\\TestFiles\\int64.bplist";
        //String  testfile = "C:\\Users\\Paul\\DevelopmentWorkspaces\\IdeaProjects\\BPList\\TestFiles\\uid.bplist";
        //String  testfile = "C:\\Users\\Paul\\DevelopmentWorkspaces\\IdeaProjects\\BPList\\TestFiles\\utf16.bplist";
        //String  testfile = "C:\\Users\\Paul\\DevelopmentWorkspaces\\IdeaProjects\\BPList\\TestFiles\\utf16_chinese.bplist";
        //String  testfile = "C:\\Users\\Paul\\DevelopmentWorkspaces\\IdeaProjects\\BPList\\TestFiles\\sample1.bplist";
        //String  testfile = "C:\\Users\\Paul\\DevelopmentWorkspaces\\IdeaProjects\\BPList\\TestFiles\\sample2.bplist";
        //String  testfile = "C:\\Users\\Paul\\DevelopmentWorkspaces\\IdeaProjects\\BPList\\TestFiles\\airplay.bplist";
        //String  testfile = "C:\\Users\\Paul\\DevelopmentWorkspaces\\IdeaProjects\\BPList\\TestFiles\\iTunes-small.bplist";
        String  testfile = "C:\\Users\\Paul\\DevelopmentWorkspaces\\IdeaProjects\\BPList\\TestFiles\\Procreate_Pop_Document.archive";

        Result<Dict> result = BPList.decode(testfile);
        if (!result.isSuccess())
        {
            System.err.println(result.getMessage());
            return;
        }

        String json = BPList.toJsonString(result.getValue());
        System.out.println(json);

    }
}
