package funclang;
import static funclang.AST.*;
import static funclang.Value.*;

import java.util.List;
import java.util.ArrayList;
import java.io.File;
import java.io.IOException;
import java.util.Random;

import funclang.Env.*;

public class Evaluator implements Visitor<Value> {

	Printer.Formatter ts = new Printer.Formatter();

	Env initEnv = initialEnv(); //New for definelang

	Value valueOf(Program p) {
			return (Value) p.accept(this, initEnv);
	}

	@Override
	public Value visit(AddExp e, Env env) {
		List<Exp> operands = e.all();
		double result = 0;
		for(Exp exp: operands) {
			NumVal intermediate = (NumVal) exp.accept(this, env); // Dynamic type-checking
			result += intermediate.v(); //Semantics of AddExp in terms of the target language.
		}
		return new NumVal(result);
	}

	@Override
	public Value visit(UnitExp e, Env env) {
		return new UnitVal();
	}

	@Override
	public Value visit(NumExp e, Env env) {
		return new NumVal(e.v());
	}

	@Override
	public Value visit(StrExp e, Env env) {
		return new StringVal(e.v());
	}

	@Override
	public Value visit(BoolExp e, Env env) {
		return new BoolVal(e.v());
	}

	@Override
	public Value visit(DivExp e, Env env) {
		List<Exp> operands = e.all();
		NumVal lVal = (NumVal) operands.get(0).accept(this, env);
		double result = lVal.v();
		for(int i=1; i<operands.size(); i++) {
			NumVal rVal = (NumVal) operands.get(i).accept(this, env);
			result = result / rVal.v();
		}
		return new NumVal(result);
	}

	@Override
	public Value visit(MultExp e, Env env) {
		List<Exp> operands = e.all();
		double result = 1;
		for(Exp exp: operands) {
			NumVal intermediate = (NumVal) exp.accept(this, env); // Dynamic type-checking
			result *= intermediate.v(); //Semantics of MultExp.
		}
		return new NumVal(result);
	}

	@Override
	public Value visit(Program p, Env env) {
		try {
			for(DefineDecl d: p.decls())
				d.accept(this, initEnv);
			return (Value) p.e().accept(this, initEnv);
		} catch (ClassCastException e) {
			return new DynamicError(e.getMessage());
		}
	}

	@Override
	public Value visit(SubExp e, Env env) {
		List<Exp> operands = e.all();
		NumVal lVal = (NumVal) operands.get(0).accept(this, env);
		double result = lVal.v();
		for(int i=1; i<operands.size(); i++) {
			NumVal rVal = (NumVal) operands.get(i).accept(this, env);
			result = result - rVal.v();
		}
		return new NumVal(result);
	}

	@Override
	public Value visit(VarExp e, Env env) {
		// Previously, all variables had value 42. New semantics.
		return env.get(e.name());
	}

	@Override
	public Value visit(LetExp e, Env env) { // New for varlang.
		List<String> names = e.names();
		List<Exp> value_exps = e.value_exps();
		List<Value> values = new ArrayList<Value>(value_exps.size());

		for(Exp exp : value_exps)
			values.add((Value)exp.accept(this, env));

		Env new_env = env;
		for (int index = 0; index < names.size(); index++)
			new_env = new ExtendEnv(new_env, names.get(index), values.get(index));

		return (Value) e.body().accept(this, new_env);
	}

	@Override
	public Value visit(DefineDecl e, Env env) { // New for definelang.
		String name = e.name();
		Exp value_exp = e.value_exp();
		Value value = (Value) value_exp.accept(this, env);
		((GlobalEnv) initEnv).extend(name, value);
		return new Value.UnitVal();
	}

	/*
	@Override
	public Value visit(LambdaExp e, Env env) { // New for funclang.
		//Check for any default parameters and add them to the environment:


		return new Value.FunVal(env, e.formals(), e.body());
	}
	*/

//======================================================================================================================

	@Override
	public Value visit(LambdaExp e, Env env) { // New for funclang.

		//I'm assuming formals and values are the same size because otherwise something is big fucked lol

		//Check for any default parameters and add them to the environment:
		for(int i = 0; i < e.formals().size(); i++){

			//The value will either be null, or the default value provided
			if(e.values().get(i) != null){
				//Grab the provided default value and its paired variable name
				Value.NumVal defaultVal = new Value.NumVal(Integer.parseInt(e.values().get(i)));
				String name = e.formals().get(i);

				//Add them to the environment
				env = new ExtendEnv(env, name, defaultVal);
			}
		}

		return new Value.FunVal(env, e.formals(), e.body());
	}


	@Override
	public Value visit(CallExp e, Env env) { // New for funclang.
		Object result = e.operator().accept(this, env);
		if(!(result instanceof Value.FunVal))
			return new Value.DynamicError("Operator not a function in call " +  ts.visit(e, env));
		Value.FunVal operator =  (Value.FunVal) result; //Dynamic checking
		List<Exp> operands = e.operands();

		// Call-by-value semantics
        //Just call them values or something. Who tf decided on 'actuals'
		List<Value> actuals = new ArrayList<Value>(operands.size());
		for(Exp exp : operands)
			actuals.add((Value)exp.accept(this, env));

		//These are names but some asshat decided to call them formals
		List<String> formals = operator.formals();

		/*	The size no longer needs to match because of default parameters, must use a different method \/\/
		if (formals.size()!=actuals.size())
			return new Value.DynamicError("Argument mismatch in call " + ts.visit(e, env));

		Env fun_env = operator.env();
		for (int index = 0; index < formals.size(); index++)
			fun_env = new ExtendEnv(fun_env, formals.get(index), actuals.get(index));
		 */

		//Because all default parameters must come after non-default ones, we can assume that any parameters not
		//covered by the passed actuals [values >:( ] are either in the environment because of the work done in
		//LambdaExp, or they are nonexistent and we should throw an error.

		//Add all passed actuals [values >:( ] to the environment
		Env fun_env = operator.env();
		int i;
		for (i = 0; i < actuals.size(); i++)
			fun_env = new ExtendEnv(fun_env, formals.get(i), actuals.get(i));

		//Now check the environment for any remaining actuals [values >:( ]
		//If the value does not already exist in the environment, throw an error
		//(I'm letting the env throw its own error when it can't find a binding for the passed name)
		for(i = i; i < formals.size(); i++){
			fun_env.get(formals.get(i));
		}


		return (Value) operator.body().accept(this, fun_env);
	}

//======================================================================================================================

	@Override
	public Value visit(IfExp e, Env env) { // New for funclang.
		Object result = e.conditional().accept(this, env);
		if(!(result instanceof Value.BoolVal))
			return new Value.DynamicError("Condition not a boolean in expression " +  ts.visit(e, env));
		Value.BoolVal condition =  (Value.BoolVal) result; //Dynamic checking

		if(condition.v())
			return (Value) e.then_exp().accept(this, env);
		else return (Value) e.else_exp().accept(this, env);
	}

//======================================================================================================================

	/** Helper method to compare length and content of two lists
	 *
	 * @param list1 First list
	 * @param list2 Second list
	 * @return 0 if both lists are equal in length and content,
	 * 			-1 if list1.length < list2.length,
	 * 		 	 1 if list1.length > list2.length
	 */
	public double compareLists(Value list1, Value list2){

		//Have we hit the end of the first list
		if(list1 instanceof Value.Null){
			if(list2 instanceof Value.Null)
				return 0;	//l1.length == l2.length
			return -1;		//l1.length < l2.length
		}
		//If we got this far, list1 is not null
		if(list2 instanceof Value.Null)
			return 1;		//l1.length > l2.length

		//Assume the values are pairs
		Value.PairVal pair1 = (Value.PairVal)list1;
		Value.PairVal pair2 = (Value.PairVal)list2;

		//Check if values are null (There can be empty lists inside a list)
		if(pair1.fst() instanceof Value.Null){
			if(!(pair2.fst() instanceof Value.Null))
				return -1;	//Pair1 == null, Pair2 != null
		}
		//If we got this far, pair1 is not null
		else if(pair2.fst() instanceof Value.Null)
			return 1;		//Pair1 != null, Pair2 == null
		//Now just check if the values are equal or not
		else if(((Value.NumVal)pair1.fst()).v() != ((Value.NumVal)pair2.fst()).v())
			return -1;		//Could also return 1 if we wanted, arbitrary as long as it's not 0


		return compareLists(pair1.snd(), pair2.snd());
	}


	@Override
	public Value visit(LessExp e, Env env) { // New for funclang.

		Object result1 = e.first_exp().accept(this, env);
		Object result2 = e.second_exp().accept(this, env);
		double firstVal;
		double secondVal;

		//If the results are NumVals
		if((result1 instanceof Value.NumVal) && (result2 instanceof Value.NumVal)){
			firstVal  = ((Value.NumVal) result1).v();
			secondVal = ((Value.NumVal) result2).v();
		}
		//If the results are StringVals
		else if((result1 instanceof Value.StringVal) && (result2 instanceof Value.StringVal)) {
			String str1 = ((Value.StringVal) result1).v();
			String str2 = ((Value.StringVal) result2).v();
			firstVal = str1.length() - str2.length();
			secondVal = 0;
		}
		//Otherwise they are PairVals
		else{
			firstVal = compareLists((Value) result1, (Value) result2);
			secondVal = 0;
		}

		return new Value.BoolVal(firstVal < secondVal);
	}

//----------------------------------------------------------------------------------------------------------------------
	@Override
	public Value visit(GreaterExp e, Env env) { // New for funclang.

		Object result1 = e.first_exp().accept(this, env);
		Object result2 = e.second_exp().accept(this, env);
		double firstVal;
		double secondVal;

		//If the results are NumVals
		if((result1 instanceof Value.NumVal) && (result2 instanceof Value.NumVal)){
			firstVal  =((Value.NumVal) result1).v();
			secondVal =((Value.NumVal) result2).v();
		}
		//If the results are StringVals
		else if((result1 instanceof Value.StringVal) && (result2 instanceof Value.StringVal)) {
			String str1 = ((Value.StringVal) result1).v();
			String str2 = ((Value.StringVal) result2).v();
			firstVal = str1.length() - str2.length();
			secondVal = 0;
		}
		//Otherwise they are PairVals
		else{
			firstVal = compareLists((Value) result1, (Value) result2);
			secondVal = 0;
		}

		return new Value.BoolVal(firstVal > secondVal);
	}

//----------------------------------------------------------------------------------------------------------------------
	@Override
	public Value visit(EqualExp e, Env env) { // New for funclang.

		Object result1 = e.first_exp().accept(this, env);
		Object result2 = e.second_exp().accept(this, env);
		double firstVal;
		double secondVal;

		//If the results are NumVals
		if((result1 instanceof Value.NumVal) && (result2 instanceof Value.NumVal)){
			firstVal  = ((Value.NumVal) result1).v();
			secondVal = ((Value.NumVal) result2).v();
		}
		//If the results are BoolVals
		else if((result1 instanceof Value.BoolVal) && (result2 instanceof Value.BoolVal)){
			firstVal  = ((BoolVal) result1).v()   ? 1 : 0;
			secondVal = ((BoolVal) result2).v()   ? 1 : 0;
		}
		//If the results are StringVals
		else if((result1 instanceof Value.StringVal) && (result2 instanceof Value.StringVal)) {
			String str1 = ((Value.StringVal) result1).v();
			String str2 = ((Value.StringVal) result2).v();
			firstVal = str1.compareTo(str2);
			secondVal = 0;
		}
		//Otherwise they are PairVals
		else{
			firstVal = compareLists((Value) result1, (Value) result2);
			secondVal = 0;
		}

		return new Value.BoolVal(firstVal == secondVal);
	}
//======================================================================================================================


	@Override
	public Value visit(CarExp e, Env env) {
		Value.PairVal pair = (Value.PairVal) e.arg().accept(this, env);
		return pair.fst();
	}

	@Override
	public Value visit(CdrExp e, Env env) {
		Value.PairVal pair = (Value.PairVal) e.arg().accept(this, env);
		return pair.snd();
	}

	@Override
	public Value visit(ConsExp e, Env env) {
		Value first = (Value) e.fst().accept(this, env);
		Value second = (Value) e.snd().accept(this, env);
		return new Value.PairVal(first, second);
	}

	@Override
	public Value visit(ListExp e, Env env) { // New for funclang.
		List<Exp> elemExps = e.elems();
		int length = elemExps.size();
		if(length == 0)
			return new Value.Null();

		//Order of evaluation: left to right e.g. (list (+ 3 4) (+ 5 4))
		Value[] elems = new Value[length];
		for(int i=0; i<length; i++)
			elems[i] = (Value) elemExps.get(i).accept(this, env);

		Value result = new Value.Null();
		for(int i=length-1; i>=0; i--)
			result = new PairVal(elems[i], result);
		return result;
	}

	@Override
	public Value visit(NullExp e, Env env) {
		Value val = (Value) e.arg().accept(this, env);
		return new BoolVal(val instanceof Value.Null);
	}

	@Override
	public Value visit(LengthStrExp e, Env env) {
		Value val = (Value) e.getStrExpr().accept(this, env);
		if (val instanceof StringVal) {
			String string = ((StringVal) val).v();
			return new NumVal(string.length() - 2); // - 2 to remove double quotes
		}

		return new DynamicError("Parameter for length was not a string.");

	}

	public Value visit(EvalExp e, Env env) {
		StringVal programText = (StringVal) e.code().accept(this, env);
		Program p = _reader.parse(programText.v());
		return (Value) p.accept(this, env);
	}

	public Value visit(ReadExp e, Env env) {
		StringVal fileName = (StringVal) e.file().accept(this, env);
		try {
			String text = Reader.readFile("" + System.getProperty("user.dir") + File.separator + fileName.v());
			return new StringVal(text);
		} catch (IOException ex) {
			return new DynamicError(ex.getMessage());
		}
	}

	private Env initialEnv() {
		GlobalEnv initEnv = new GlobalEnv();

		/* Procedure: (read <filename>). Following is same as (define read (lambda (file) (read file))) */
		List<String> formals = new ArrayList<>();
		formals.add("file");
		Exp body = new AST.ReadExp(new VarExp("file"));
		Value.FunVal readFun = new Value.FunVal(initEnv, formals, body);
		initEnv.extend("read", readFun);

		/* Procedure: (require <filename>). Following is same as (define require (lambda (file) (eval (read file)))) */
		formals = new ArrayList<>();
		formals.add("file");
		body = new EvalExp(new AST.ReadExp(new VarExp("file")));
		Value.FunVal requireFun = new Value.FunVal(initEnv, formals, body);
		initEnv.extend("require", requireFun);

		/* Add new built-in procedures here */
		formals = new ArrayList<>();
		formals.add("str");
		body = new AST.LengthStrExp(new VarExp("str"));
		Value.FunVal lengthFun = new Value.FunVal(initEnv, formals, body);
		initEnv.extend("length", lengthFun);

		return initEnv;
	}

	Reader _reader;
	public Evaluator(Reader reader) {
		_reader = reader;
	}
}
