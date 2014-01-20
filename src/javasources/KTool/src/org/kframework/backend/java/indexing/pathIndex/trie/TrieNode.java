package org.kframework.backend.java.indexing.pathIndex.trie;

import java.util.ArrayList;

/**
 * Author: Owolabi Legunsen
 * 1/2/14: 7:23 PM
 */
public class TrieNode {
    private String value;
    private ArrayList<TrieNode> children;

    TrieNode() {
    }

    public TrieNode(String value) {
        this.value = value;
    }

    public boolean inChildren(String value){
        boolean contained = false;
        for (TrieNode node:getChildren()){
            if (node.getValue().equals(value)){
                contained = true;
                break;
            }
        }
        return contained;
    }

    public TrieNode getChild(String value){
        TrieNode child = null;
        for (TrieNode node: getChildren()){
            if(node.getValue().equals(value)){
                child = node;
            }
        }
        return child;
    }

//    public TrieNode getChild(TrieNode startNode,String pString, int position){
//        TrieNode child = startNode;
//        int i = 0;
//        // seek the child node containing the value just before position in pString
//        while(i <= position){
////            child = child.getChild(pString.charAt(i));
//            i++;
//        }
//
//        return child;
//    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public ArrayList<TrieNode> getChildren() {
        return children;
    }

    public void setChildren(ArrayList<TrieNode> children) {
        this.children = children;
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TrieNode trieNode = (TrieNode) o;

        if (children != null ? !children.equals(trieNode.children) : trieNode.children != null)
            return false;
        return value.equals(trieNode.value);

    }

    @Override
    public int hashCode() {
        int result = value.hashCode();
        result = 31 * result + (children != null ? children.hashCode() : 0);
        return result;
    }
}
