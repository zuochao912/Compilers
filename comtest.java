import java.util.Arrays;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.gui.TreeViewer;

public class comtest {
	public static void main( String[] args) throws Exception 
	{

		ANTLRInputStream input = new ANTLRInputStream( System.in);

		CLexer lexer = new CLexer(input);

		CommonTokenStream tokens = new CommonTokenStream(lexer);
		
		CParser parser = new CParser(tokens);
		ParseTree tree = parser.compilationUnit(); // begin parsing at rule compilationUnit
//		ParseTree tree=parser.blockItemList();  //临时使用的
//		System.out.println(tree.toStringTree(parser)); // print LISP-style tree
      
		TreeViewer viewr = new TreeViewer(Arrays.asList(parser.getRuleNames()),tree);
		showtree(viewr);
		ParseTreeWalker walker = new ParseTreeWalker();
		//step2:创建ParseTreeListener和ParseTree
		walker.walk(new realListener(), tree); 
		/*X86Generator gen1=new X86Generator();
		gen1.filereader("C:\\Users\\DELL\\Desktop\\编译lab8\\IR\\2.IR.txt");*/
	}
	private static void showtree(TreeViewer treev) {
		// show AST in GUI
			JFrame frame = new JFrame("Antlr AST");
			JPanel panel = new JPanel();
			treev.setScale(0.8);//scale a little
			panel.add(treev);
			frame.add(panel);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setSize(300,300);
			frame.setVisible(true);
	}
}
