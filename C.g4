grammar C;

compilationUnit  //增广文法
    :   translationUnit EOF
    ;

translationUnit  //语句块
    :   externalDeclaration+
    ;

externalDeclaration  //外部声明，即全局意义下
    :   functionDefinition  //函数定义
    |   declaration  //函数声明或者变量声明
    |   ';' // stray ;
    ;

functionDefinition    //函数定义,返回类型+标识符+参数表+函数体 ,原本参数表是有问号的;
    :   typespecifier directDeclarator  compoundStatement
    ;
    
declaration:   typespecifier initDeclaratorList? ';' ; //一句定义，意味着只有一种类型！

directDeclarator  //声明部分的标识符部分
    :   Identifier   #direcid      //int a的a
    |   '(' directDeclarator ')'   #direcRec
    |   directDeclarator '['assignmentExpression? ']'  #direcArr //int a[8][8]这种，数组声明没问题，此处采用左递归策略
    |   directDeclarator '(' parameterList? ')'   #direcFunc  //函数声明
//    | directDeclarator '(' ')'  # direcFunc2 
    ;
    
typespecifier:   'void'|   'char'|   'short'|   'int'|   'long'|   'float'|   'double'|   'signed'|   'unsigned';

initDeclaratorList  //用于表述int a,b,c这种情况
    :   initDeclarator cormaDeclarator  #initDecext  //用于延续int a,b,c;
    |   initDeclarator       #initDecend
    ;

cormaDeclarator: ',' initDeclarator cormaDeclarator  #cormaext//将原本的initDeclarator改写过了
		| ',' initDeclarator              #cormaend;


    
initDeclarator   
    :   directDeclarator '=' initializer #initequ //如果存在int a=1,初始化；
    | directDeclarator  #initdirec  //int a,b的a\b
    ;

initializer
    :   assignmentExpression       #initval  // 变量初始化列表中的一项，初始化变量,a={1,2,3,4,5}
    |   '{' initializerList ','? '}'  #initlist//初始化列表
    ;

initializerList
    :    initializer inittializerListext #initlistext //原本的desination暂时没用
    |  initializer  #initlistend
    ;
    
inittializerListext: ','  initializer inittializerListext  #initlistextext
	|	','  initializer  #initlistextend //将上述的文法改写过，分为两条
;



parameterList    //函数定义、声明时的参数列表
    :   parameterDeclaration  parameterListext 
    |   parameterDeclaration   
    ;
    
parameterListext : ',' parameterDeclaration  parameterListext  #paralistextext
				| ',' parameterDeclaration #paralistextend;

parameterDeclaration  //函数参数表中的定义(int a=5,b=4)
    :   typespecifier directDeclarator  //int a这种，int a=5
    ;
    
//以下均为算术部分，不包括分支、选择

compoundStatement   //语句块，带大括号，防止空语句块而已。
    :   '{' blockItemList? '}'
    ;

blockItemList   //语句块内容，不带大括号
    :   blockItem+  //语句
    ;


blockItem
    :   statement   //非定义的语句
    |   declaration  //定义部分
    ;
    
statement   //一句语句，流程控制、条件、语句块，不包括定义、声明！
    :   compoundStatement
//    |   labeledStatement
    |   expressionStatement
    |   selectionStatement
    |   iterationStatement
    |   jumpStatement ;

//labeledStatement :   Identifier ':' statement ;
    
jumpStatement   //流程跳转语句
    :   'goto' Identifier  ';' #goto
    |  'break' ';' #break
    |  'continue'  ';' #continue
    |   'return' expression? ';' #return
    ;
expressionStatement //特别小心空语句，仅此而已
    :   expression? ';'         #exp_maybe
    |  Identifier ':' statement #label
    ;

selectionStatement  //分支语句，if-else
    :   'if' '(' expression ')' statement elseStatement #if_else
    | 'if' '(' expression ')' statement  #if_single
    ;
    
elseStatement: 'else' statement ; //此处是我多加的

iterationStatement  //循环语句
    :   While '(' expression ')' statement   #while
    |   Do statement While '(' expression ')' ';' #do_while
    |   For '(' forCondition ')' statement #forloop
    ;
forCondition   
	:   (forDeclaration | forExpression?) ';' forExpression? ';' forExpression? //这个declar部分算是初始化还是表达式
	;

forDeclaration
    :   typespecifier initDeclaratorList?
    ;

forExpression
    :   assignmentExpression (',' assignmentExpression)*
    ;

primaryExpression   //含义是不可分割的部分吧
    :   Identifier   #to_id
    |   Constant    # to_const
    |   STRING   #to_string
    |   '(' expression ')'  # newexp
    ;

postfixExpression   //前缀运算符，优先级2
    : primaryExpression  #to_primary 
    | primaryExpression   arrlist    #array
    | primaryExpression ('(' argumentExpressionList? ')')* #funcall
    | primaryExpression (OP=('.' | '->')  Identifier)*   #member
    | primaryExpression (OP=('++' | '--') )*   #postinc
    ;

arrlist: '[' expression ']' arrlist #arraygo   //数组运算
	|   #arre;
	
argumentExpressionList    //调用函数参数列表
    :   assignmentExpression ',' argumentExpressionList  #arglist
    |  logicalOrExpression   #argend
    ;

unaryExpression  //单目/前缀运算符，优先级1、2
    : postfixExpression     #nosuff
    |unaryOperator castExpression  #unary
     | OP=('++' |  '--')  (postfixExpression  |   unaryOperator castExpression )  #suffinc
    ;

unaryOperator   
    :   '&' | '*' | '+' | '-' | '~' | '!'
    ;

castExpression    //优先级为2的运算
    :    unaryExpression  
    	| Integer ;

multiplicativeExpression  //乘法运算，优先级3
    :   multiplicativeExpression OP=('*'|'/'|'%') castExpression #mul
    |   castExpression  #mulend
    ;
 
additiveExpression   //加法运算，优先级4
    :  additiveExpression OP=('+'|'-') multiplicativeExpression  #add
    |   multiplicativeExpression #addend
    ;

shiftExpression   //位移运算，优先级5
    :   additiveExpression OP=('<<'|'>>') additiveExpression #shift
    |  additiveExpression   #shiftend
    ;
    
relationalExpression   //关系运算，优先级6
    :  relationalExpression OP=('<'|'>'|'<='|'>=') shiftExpression#relop
    | shiftExpression #relopend
    ;

equalityExpression   //等于、不等与关系，优先级7
    :   equalityExpression OP=('=='| '!=') relationalExpression #equ
    |  relationalExpression  #equend
    ;

logicalAndExpression   //逻辑与运算，优先级11
    :   logicalAndExpression '&&' equalityExpression  #and
    | equalityExpression  #andend
    ;

logicalOrExpression //逻辑或运算，优先级11
    :   logicalOrExpression  '||' logicalAndExpression #or
    | logicalAndExpression #orend
    ;


assignmentExpression  //赋值运算，优先级12
    :   logicalOrExpression  #assignend
    |  unaryExpression assignmentOperator assignmentExpression #assign
    ;

assignmentOperator  //赋值符号
    :   '=' | '*=' | '/=' | '%=' | '+=' | '-=' | '<<=' | '>>=' | '&=' | '^=' | '|='
    ;

expression   //表达式，不带分号，不是语句！理论上应该处理为右递归。。不允许一棵树挂多个运算
    :   assignmentExpression (',' assignmentExpression)*
    ;



//关键字单独列出
Auto : 'auto';
Break : 'break';
Case : 'case';
Char : 'char';
Const : 'const';
Continue : 'continue';
Default : 'default';
Do : 'do';
Double : 'double';
Else : 'else';
Enum : 'enum';
Extern : 'extern';
Float : 'float';
For : 'for';
Goto : 'goto';
If : 'if';
Inline : 'inline';
Int : 'int';
Long : 'long';
Register : 'register';
Restrict : 'restrict';
Return : 'return';
Short : 'short';
Signed : 'signed';
Sizeof : 'sizeof';
Static : 'static';
Struct : 'struct';
Switch : 'switch';
Typedef : 'typedef';
Union : 'union';
Unsigned : 'unsigned';
Void : 'void';
Volatile : 'volatile';
While : 'while';

//括号运算符单独列出！
LeftParen : '(';
RightParen : ')';
LeftBracket : '[';
RightBracket : ']';
LeftBrace : '{';
RightBrace : '}';
//关系运算符
Less : '<';
LessEqual : '<=';
Greater : '>';
GreaterEqual : '>=';
//位运算
LeftShift : '<<';
RightShift : '>>';
//算术运算符，自增自减也单独列出
Plus : '+';
PlusPlus : '++';
Minus : '-';
MinusMinus : '--';
Star : '*';
Div : '/';
Mod : '%';

Question : '?';
Colon : ':';
Semi : ';';
Comma : ',';

//剩余两个关系运算符
Equal : '==';
NotEqual : '!=';

//懒惰的等于运算符

Assign : '='| '*=' | '/=' | '%=' | '+=' | '-=' | '<<=' | '>>=' | '&=' | '^=' | '|=';


//成员运算符
Arrow : '->';
Dot : '.';
Ellipsis : '...';


Identifier : [_a-zA-Z]([_a-zA-Z0-9])* ;


Constant   //常量词法
    :   Integer //整形
    |   FLOAT  //浮点型
    |  CHAR  //字符型
    ;

FLOAT : DIGIT* '.' DIGIT+;
Integer : DIGIT+;
CHAR : '\'' . '\'';
ESC: '\\"' | '\\\\';
STRING : '"' (ESC|. )*? '"' ;


SPACE	:	(' '|'\t'|'\r'|'\n') -> skip ;
LINE_COMMENT : '//' .*? '\n' -> skip ;
COMMENT :'/*' .*? '*/' -> skip ;

fragment
DIGIT	:	[0-9];
