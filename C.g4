grammar C;

compilationUnit  //�����ķ�
    :   translationUnit EOF
    ;

translationUnit  //����
    :   externalDeclaration+
    ;

externalDeclaration  //�ⲿ��������ȫ��������
    :   functionDefinition  //��������
    |   declaration  //�����������߱�������
    |   ';' // stray ;
    ;

functionDefinition    //��������,��������+��ʶ��+������+������ ,ԭ�������������ʺŵ�;
    :   typespecifier directDeclarator  compoundStatement
    ;
    
declaration:   typespecifier initDeclaratorList? ';' ; //һ�䶨�壬��ζ��ֻ��һ�����ͣ�

directDeclarator  //�������ֵı�ʶ������
    :   Identifier   #direcid      //int a��a
    |   '(' directDeclarator ')'   #direcRec
    |   directDeclarator '['assignmentExpression? ']'  #direcArr //int a[8][8]���֣���������û���⣬�˴�������ݹ����
    |   directDeclarator '(' parameterList? ')'   #direcFunc  //��������
//    | directDeclarator '(' ')'  # direcFunc2 
    ;
    
typespecifier:   'void'|   'char'|   'short'|   'int'|   'long'|   'float'|   'double'|   'signed'|   'unsigned';

initDeclaratorList  //���ڱ���int a,b,c�������
    :   initDeclarator cormaDeclarator  #initDecext  //��������int a,b,c;
    |   initDeclarator       #initDecend
    ;

cormaDeclarator: ',' initDeclarator cormaDeclarator  #cormaext//��ԭ����initDeclarator��д����
		| ',' initDeclarator              #cormaend;


    
initDeclarator   
    :   directDeclarator '=' initializer #initequ //�������int a=1,��ʼ����
    | directDeclarator  #initdirec  //int a,b��a\b
    ;

initializer
    :   assignmentExpression       #initval  // ������ʼ���б��е�һ���ʼ������,a={1,2,3,4,5}
    |   '{' initializerList ','? '}'  #initlist//��ʼ���б�
    ;

initializerList
    :    initializer inittializerListext #initlistext //ԭ����desination��ʱû��
    |  initializer  #initlistend
    ;
    
inittializerListext: ','  initializer inittializerListext  #initlistextext
	|	','  initializer  #initlistextend //���������ķ���д������Ϊ����
;



parameterList    //�������塢����ʱ�Ĳ����б�
    :   parameterDeclaration  parameterListext 
    |   parameterDeclaration   
    ;
    
parameterListext : ',' parameterDeclaration  parameterListext  #paralistextext
				| ',' parameterDeclaration #paralistextend;

parameterDeclaration  //�����������еĶ���(int a=5,b=4)
    :   typespecifier directDeclarator  //int a���֣�int a=5
    ;
    
//���¾�Ϊ�������֣���������֧��ѡ��

compoundStatement   //���飬�������ţ���ֹ��������ѡ�
    :   '{' blockItemList? '}'
    ;

blockItemList   //�������ݣ�����������
    :   blockItem+  //���
    ;


blockItem
    :   statement   //�Ƕ�������
    |   declaration  //���岿��
    ;
    
statement   //һ����䣬���̿��ơ����������飬���������塢������
    :   compoundStatement
//    |   labeledStatement
    |   expressionStatement
    |   selectionStatement
    |   iterationStatement
    |   jumpStatement ;

//labeledStatement :   Identifier ':' statement ;
    
jumpStatement   //������ת���
    :   'goto' Identifier  ';' #goto
    |  'break' ';' #break
    |  'continue'  ';' #continue
    |   'return' expression? ';' #return
    ;
expressionStatement //�ر�С�Ŀ���䣬���˶���
    :   expression? ';'         #exp_maybe
    |  Identifier ':' statement #label
    ;

selectionStatement  //��֧��䣬if-else
    :   'if' '(' expression ')' statement elseStatement #if_else
    | 'if' '(' expression ')' statement  #if_single
    ;
    
elseStatement: 'else' statement ; //�˴����Ҷ�ӵ�

iterationStatement  //ѭ�����
    :   While '(' expression ')' statement   #while
    |   Do statement While '(' expression ')' ';' #do_while
    |   For '(' forCondition ')' statement #forloop
    ;
forCondition   
	:   (forDeclaration | forExpression?) ';' forExpression? ';' forExpression? //���declar�������ǳ�ʼ�����Ǳ��ʽ
	;

forDeclaration
    :   typespecifier initDeclaratorList?
    ;

forExpression
    :   assignmentExpression (',' assignmentExpression)*
    ;

primaryExpression   //�����ǲ��ɷָ�Ĳ��ְ�
    :   Identifier   #to_id
    |   Constant    # to_const
    |   STRING   #to_string
    |   '(' expression ')'  # newexp
    ;

postfixExpression   //ǰ׺����������ȼ�2
    : primaryExpression  #to_primary 
    | primaryExpression   arrlist    #array
    | primaryExpression ('(' argumentExpressionList? ')')* #funcall
    | primaryExpression (OP=('.' | '->')  Identifier)*   #member
    | primaryExpression (OP=('++' | '--') )*   #postinc
    ;

arrlist: '[' expression ']' arrlist #arraygo   //��������
	|   #arre;
	
argumentExpressionList    //���ú��������б�
    :   assignmentExpression ',' argumentExpressionList  #arglist
    |  logicalOrExpression   #argend
    ;

unaryExpression  //��Ŀ/ǰ׺����������ȼ�1��2
    : postfixExpression     #nosuff
    |unaryOperator castExpression  #unary
     | OP=('++' |  '--')  (postfixExpression  |   unaryOperator castExpression )  #suffinc
    ;

unaryOperator   
    :   '&' | '*' | '+' | '-' | '~' | '!'
    ;

castExpression    //���ȼ�Ϊ2������
    :    unaryExpression  
    	| Integer ;

multiplicativeExpression  //�˷����㣬���ȼ�3
    :   multiplicativeExpression OP=('*'|'/'|'%') castExpression #mul
    |   castExpression  #mulend
    ;
 
additiveExpression   //�ӷ����㣬���ȼ�4
    :  additiveExpression OP=('+'|'-') multiplicativeExpression  #add
    |   multiplicativeExpression #addend
    ;

shiftExpression   //λ�����㣬���ȼ�5
    :   additiveExpression OP=('<<'|'>>') additiveExpression #shift
    |  additiveExpression   #shiftend
    ;
    
relationalExpression   //��ϵ���㣬���ȼ�6
    :  relationalExpression OP=('<'|'>'|'<='|'>=') shiftExpression#relop
    | shiftExpression #relopend
    ;

equalityExpression   //���ڡ��������ϵ�����ȼ�7
    :   equalityExpression OP=('=='| '!=') relationalExpression #equ
    |  relationalExpression  #equend
    ;

logicalAndExpression   //�߼������㣬���ȼ�11
    :   logicalAndExpression '&&' equalityExpression  #and
    | equalityExpression  #andend
    ;

logicalOrExpression //�߼������㣬���ȼ�11
    :   logicalOrExpression  '||' logicalAndExpression #or
    | logicalAndExpression #orend
    ;


assignmentExpression  //��ֵ���㣬���ȼ�12
    :   logicalOrExpression  #assignend
    |  unaryExpression assignmentOperator assignmentExpression #assign
    ;

assignmentOperator  //��ֵ����
    :   '=' | '*=' | '/=' | '%=' | '+=' | '-=' | '<<=' | '>>=' | '&=' | '^=' | '|='
    ;

expression   //���ʽ�������ֺţ�������䣡������Ӧ�ô���Ϊ�ҵݹ顣��������һ�����Ҷ������
    :   assignmentExpression (',' assignmentExpression)*
    ;



//�ؼ��ֵ����г�
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

//��������������г���
LeftParen : '(';
RightParen : ')';
LeftBracket : '[';
RightBracket : ']';
LeftBrace : '{';
RightBrace : '}';
//��ϵ�����
Less : '<';
LessEqual : '<=';
Greater : '>';
GreaterEqual : '>=';
//λ����
LeftShift : '<<';
RightShift : '>>';
//����������������Լ�Ҳ�����г�
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

//ʣ��������ϵ�����
Equal : '==';
NotEqual : '!=';

//����ĵ��������

Assign : '='| '*=' | '/=' | '%=' | '+=' | '-=' | '<<=' | '>>=' | '&=' | '^=' | '|=';


//��Ա�����
Arrow : '->';
Dot : '.';
Ellipsis : '...';


Identifier : [_a-zA-Z]([_a-zA-Z0-9])* ;


Constant   //�����ʷ�
    :   Integer //����
    |   FLOAT  //������
    |  CHAR  //�ַ���
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
