package org.snapscript.parse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class SyntaxTree {

   private final Comparator<SyntaxNode> comparator;
   private final List<SyntaxCursor> nodes;
   private final LexicalAnalyzer analyzer;
   private final GrammarIndexer indexer;
   private final AtomicInteger commit;
   private final PositionStack stack;
   private final String resource;
   private final String grammar;
   private final int length;

   public SyntaxTree(GrammarIndexer indexer, String resource, String grammar, char[] original, char[] source, short[] lines, short[] types) {
      this.analyzer = new TokenScanner(indexer, resource, original, source, lines, types);
      this.comparator = new SyntaxNodeComparator();
      this.nodes = new LinkedList<SyntaxCursor>();
      this.commit = new AtomicInteger();
      this.stack = new PositionStack();
      this.length = source.length;
      this.resource = resource;
      this.indexer = indexer;
      this.grammar = grammar;
   } 
   
   public SyntaxChecker check() {   
      int index = indexer.index(grammar);
      int depth = stack.depth(0, index);

      if (depth >= 0) {
         throw new ParseException("Syntax has been validated");
      }
      stack.push(0, index);
      return new SyntaxValidator(analyzer);
   }

   public SyntaxBuilder build() {   
      int index = indexer.index(grammar);
      int mark = analyzer.mark();
      int count = analyzer.count();
      
      if(mark != count) {
         int error = commit.get(); // last successful commit
         Line line = analyzer.line(error);
         
         if(resource != null) {
            throw new ParseException("Syntax error in '" + resource + "' at line " + line);
         }  
         throw new ParseException("Syntax error at line " + line);
      }
      analyzer.reset(0);
      stack.clear();
      stack.push(0,index);
      return new SyntaxCursor(analyzer, nodes, index, 0);
   }
   
   public SyntaxNode commit() { 
      int size = nodes.size();
      
      if(size > 2) {
         throw new ParseException("Syntax tree has more than one root");
      }
      SyntaxCursor cursor = nodes.get(0);
      SyntaxNode node = cursor.create();
      
      if(node == null) {
         throw new ParseException("Syntax tree has no root");
      }
      return node;
   }
   
   public int length() {
      return length;
   }
   
   private class SyntaxValidator extends TokenConsumer implements SyntaxChecker {

      public SyntaxValidator(LexicalAnalyzer analyzer) {
         this.analyzer = analyzer;
      }        
      
      @Override
      public void validate(){
         int mark = analyzer.mark();
         int count = analyzer.count();
         
         if(mark != count) {
            int error = commit.get(); // last successful commit
            Line line = analyzer.line(error);
            
            if(resource != null) {
               throw new ParseException("Syntax error in '" + resource + "' at line " + line);
            }  
            throw new ParseException("Syntax error at line " + line);
         }
      }
      
      @Override
      public int mark(int grammar) {              
         int off = analyzer.mark();
         int index = stack.depth(off, grammar); // this is slow!!

         if (index <= 0) {
            stack.push(off, grammar);
            return off;
         }
         return -1;
      }  

      @Override
      public int reset(int start, int grammar) {
         int mark = analyzer.mark();
            
         stack.pop(start, grammar);
         analyzer.reset(start); // sets the global offset
         return mark;
      }

      @Override
      public void commit(int start, int grammar) {
         int mark = analyzer.mark();
         int error = commit.get();
         int value = stack.pop(start, grammar);

         if (value != -1) {
            if(mark > error) {
               commit.set(mark);
            }
         }
      }

      @Override
      public int position() {
         return analyzer.mark();
      }
      
      @Override
      public int peek() {
         return analyzer.peek();
      }
   }
   
   private class SyntaxCursor extends TokenConsumer implements SyntaxBuilder {

      private final List<SyntaxCursor> parent;
      private final List<SyntaxCursor> nodes;
      private final int grammar;
      private final int start;

      public SyntaxCursor(LexicalAnalyzer analyzer, List<SyntaxCursor> parent, int grammar, int start) {
         this.nodes = new LinkedList<SyntaxCursor>();
         this.analyzer = analyzer;
         this.grammar = grammar;
         this.parent = parent;
         this.start = start;
      }      
      
      public SyntaxNode create() {
         return new SyntaxResult(nodes, value, grammar, start);
      }

      @Override
      public SyntaxBuilder mark(int grammar) {              
         int off = analyzer.mark();
         int index = stack.depth(off, grammar); // this is slow!!

         if (index <= 0) {
            stack.push(off, grammar);
            return new SyntaxCursor(analyzer, nodes, grammar, off);
         }
         return null;
      }    

      @Override
      public int reset() {
         int mark = analyzer.mark();
            
         stack.pop(start, grammar);
         analyzer.reset(start); // sets the global offset
         return mark;
      }

      @Override
      public void commit() {
         int mark = analyzer.mark();
         int error = commit.get();
         int value = stack.pop(start, grammar);

         if (value != -1) {
            if(mark > error) {
               commit.set(mark);
            }
            parent.add(this);
         }
      }

      @Override
      public int position() {
         return analyzer.mark();
      }
      
      @Override
      public int peek() {
         return analyzer.peek();
      }
   }

   private class SyntaxResult implements SyntaxNode {

      private List<SyntaxCursor> nodes;
      private Token token;
      private int grammar;
      private int start;

      public SyntaxResult(List<SyntaxCursor> nodes, Token token, int grammar, int start) {
         this.grammar = grammar;
         this.token = token;
         this.start = start;
         this.nodes = nodes;
      }

      @Override
      public List<SyntaxNode> getNodes() {
         int size = nodes.size();
         
         if(size > 0) {
            List<SyntaxNode> result = new ArrayList<SyntaxNode>(size);
            
            for(SyntaxCursor child : nodes) {
               SyntaxNode node = child.create();
               
               if(node != null) {
                  result.add(node);
               }
            }         
            if(size > 1) {
               Collections.sort(result, comparator);
            }
            return result;
         }
         return Collections.emptyList();
      }

      @Override
      public String getGrammar() {
         return indexer.value(grammar);
      }
      
      @Override
      public Line getLine() {
         return analyzer.line(start);
      }

      @Override
      public Token getToken() {
         return token;
      }

      @Override
      public int getStart() {
         return start;
      }
      
      @Override
      public String toString(){
         return indexer.value(grammar);
      }
   } 
}