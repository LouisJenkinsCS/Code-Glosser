/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bloomu.codeglosser.HTML;

/**
 *
 * @author Louis
 */
public class Java2HTML extends Lang2HTML {

    @Override
    protected String[] getKeywordList() {
        return new String[] {"abstract", "continue", "for", "new", "switch",
            "assert", "default", "goto", "package", "synchronized",
            "boolean",	"do",	"if",	"private",	"this",
            "break",	"double",	"implements",	"protected",	"throw",
            "byte",	"else",	"import",	"public",	"throws",
            "case",	"enum",	"instanceof",	"return",	"transient",
            "catch",	"extends",	"int",	"short",	"try",
            "char",	"final",	"interface",	"static",	"void",
            "class",	"finally",	"long",	"strictfp",	"volatile",
            "const",	"float",	"native",	"super",	"while"};
    }

    @Override
    protected String[] getPrimitiveTypeList() {
        return new String[]{};
    }
    
}
