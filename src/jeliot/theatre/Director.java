package jeliot.theatre;

import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import jeliot.lang.*;
import jeliot.gui.*;
import jeliot.*;
import jeliot.ecode.*;

//import jeliot.parser.*;

/**
  * @author Pekka Uronen
  *
  * created         12.8.1999
  * revised         18.9.1999
  * modified        12.12.2002 by Niko Myller
  */
public class Director {

    /** True, if the director should stop after executing one statement. */
    private boolean stepByStep;

    /** Theatre to show the animation in. */
    private Theatre theatre;

    /** Pane showing the code. For highlighting. */
    private CodePane codePane;

    /** Master Jeliot. */
    private Jeliot jeliot;

    /** Factory that produces the actors. */
    private ActorFactory factory;

    /** */
    private TheatreManager manager;

    /** */
    private AnimationEngine engine;

    /** Taken of for Jeliot 3 */
//  private PMethod mainMethod;
    private MethodFrame currentMethodFrame;

    private Scratch currentScratch;
    private ConstantBox cbox;
    private ConstantBox input;

    private ThreadController controller;

    private Stack scratchStack = new Stack();
    private Stack frameStack = new Stack();

    /** Not needed in Jeliot 3 */
    //private Stack exprStack = new Stack();

    private Interpreter eCodeInterpreter;

    public Director(
            Theatre theatre,
            CodePane codePane,
            Jeliot jeliot,
            AnimationEngine engine) {

        this.theatre = theatre;
        this.codePane = codePane;
        this.jeliot = jeliot;
        this.engine = engine;

        this.manager = theatre.getManager();
    }

    public void setController(ThreadController controller) {
        this.controller = controller;
    }

    public Scratch getCurrentScratch() {
        return currentScratch;
    }

    public void setActorFactory(ActorFactory factory) {
        this.factory = factory;
    }

//  public void setMainMethod(PMethod main) {
//      this.mainMethod = main;
//  }

    public MethodFrame getCurrentMethodFrame() {
        return currentMethodFrame;
    }

    public void direct() throws Exception {
        cbox = factory.produceConstantBox();
        input = factory.produceInputBox();
        theatre.addPassive(cbox);
        theatre.getManager().setConstantBox(cbox);

        //theatre.addPassive(input);
        //theatre.getManager().setInputBox(input);

        //Commented for Jeliot 3
//      mainMethod.execute(this);

        //Excecution of the program code takes place here.
        eCodeInterpreter.execute();

        highlight(new Highlight(0,0,0,0));
        theatre.repaint();
    }

    //Parameter Values need to be changed
//     public void atStatement(PStatement sta) {
//         if (stepByStep) {
//             jeliot.directorPaused();
//             controller.checkPoint();
//         }

//         highlight(sta.getLeftPos(), sta.getRightPos());

//     }

    public void setStep(boolean step) {
        this.stepByStep = step;
    }

    //Parameter Values need to be changed
//     public void atExpression(PExpression ex) {
//         highlight(ex.getLeftPos(), ex.getRightPos());
//     }

    //Parameter Values need to be changed
//     public void atDeclarator(PVariableDeclarator decl) {
//         highlight(decl.getLeftPos(), decl.getRightPos());
//     }

    //Changed for Jeliot 3
    public void highlight(Highlight h) {
        if (!eCodeInterpreter.starting()) {
            if (stepByStep) {
                jeliot.directorPaused();
                controller.checkPoint();
            }
            if (h != null) {
                codePane.highlightStatement(h);
            }
        }
    }

    public void openScratch() {
        if (currentScratch != null) {
            scratchStack.push(currentScratch);
        }
        currentScratch = new Scratch();
        theatre.getManager().addScratch(currentScratch);
//        return currentScratch;
    }

    public void closeScratch() {
        if (currentScratch != null) {
            currentScratch.removeCrap();
            theatre.getManager().removeScratch(currentScratch);
            theatre.repaint();
            if (!scratchStack.empty()) {
                currentScratch = (Scratch) scratchStack.pop();
            }
        }
    }

    public void closeExpression() {

    }


    /** This method animates the first half of a binary expression.
      * For example in expression  a + b  this will animate as
      * (supposing that the value of a is 1):   1 + ...
      *
      *@returns The expression actor which the expression is put on.
      */
    public ExpressionActor beginBinaryExpression(
            Value operand,
            int operator, int expressionReference, Highlight h) {

        highlight(h);

        // Prepare the actors
        ValueActor operandAct = operand.getActor();
        OperatorActor operatorAct =
                factory.produceBinOpActor(operator);
        Actor dotsAct = factory.produceEllipsis();

        // Create the expression actor for 5 elements and reserve
        // places for the three first actors.
        ExpressionActor expr = currentScratch.getExpression(5, expressionReference);
            Point operandLoc    = expr.reserve(operandAct);
            Point operatorLoc   = expr.reserve(operatorAct);
            Point dotsLoc       = expr.reserve(dotsAct);

        // Prepare the theatre for animation.
        theatre.capture();

        // Move the first operand to its place.
        engine.showAnimation(operandAct.fly(operandLoc));
        expr.bind(operandAct);

        // Make the operator appear.
        engine.showAnimation(operatorAct.appear(operatorLoc));
        expr.bind(operatorAct);

        // Make the ellipsis appear.
        engine.showAnimation(dotsAct.appear(dotsLoc));
        expr.bind(dotsAct);

        // Re-activate the theatre after animation.
        theatre.release();

        return expr;
    }


    //Added for Jeliot 3
    public void rightBinaryExpression(Value operand, ExpressionActor expr, Highlight h) {

        highlight(h);

        // Prepare the theatre for animation.
        theatre.capture();

        // If there is a second operand, remove the ellipsis and
        // replace them with the second operand.
        if (operand != null) {
            // Get the operand's actor.
            ValueActor operandAct = operand.getActor();

            // Remove the ellipsis and reserve its place for the second
            // operand.
            expr.cut();
            Point operandLoc = expr.reserve(operandAct);

            // Move the operand to its place.
            engine.showAnimation(operandAct.fly(operandLoc));
            expr.bind(operandAct);
        }

        // De-activate the theatre.
        theatre.release();
    }

    /** Animates the second part of a binary expression.
      *
      *@param operand The second operand for the binary expression. If
      *     this is null, it means that the second operand was not
      *     evaluated, as in case false and ...
      */
    public Value finishBinaryExpression(
            Value result,
            int operator,
            ExpressionActor expr,
            Highlight h) {

        highlight(h);

        // Prepare the actors.
        ValueActor resultAct = factory.produceValueActor(result);
        OperatorActor operatorAct =
                factory.produceBinOpResActor(operator);


        // Prepare the theatre for animation.
        theatre.capture();

        // Reserve places for the equals sign and the result.
        Point operatorLoc = expr.reserve(operatorAct);
        Point resultLoc = expr.reserve(resultAct);

        // Make the equals sign (operator) appear.
        engine.showAnimation(operatorAct.appear(operatorLoc));
        expr.bind(operatorAct);

        // Make the result appear.
        engine.showAnimation(resultAct.appear(resultLoc));
        expr.bind(resultAct);

        // Make the expression dark.
        expr.setLight(Actor.SHADED);

        // Create and set a new actor for the result.
        ValueActor clone = (ValueActor)resultAct.clone();
        clone.setLight(Actor.NORMAL);
        clone.setLocation(resultAct.getRootLocation());
        currentScratch.registerCrap(clone);
        theatre.addPassive(clone);
        result.setActor(clone);

        // De-activate the theatre.
        theatre.release();

        Value val = (Value) result.clone();
        ValueActor rAct = factory.produceValueActor(val);
        rAct.setLight(Actor.NORMAL);
        rAct.setLocation(resultAct.getRootLocation());
        val.setActor(rAct);

        return val;
    }

    /** Shows an animation of the invocation of a static foreign
      * method.
      */
//     public Return animateSFMInvocation(
//             ForeignMethodPointer method,
//             Value[] args) {

//         // Get animator for the invocation.
//         Animator animator = method.getAnimator(args);

//         // Get actors for the arguments.
//         int n = args.length;
//         ValueActor[] actors = new ValueActor[n];
//         for (int i = 0; i < n; ++i) {
//             actors[i] = args[i].getActor();
//         }
//         // Animate the invocation
//         animator.setArguments(args);
//         animator.setArgumentActors(actors);
//         animator.animate(this);

//         return new Return(animator.getReturnValue());
//     }


    /** Animates the invocation of a domestic (user-defined) method.
      */
    public Value[] animateSMInvocation(String methodName,
            //DomesticMethodPointer dmp,
            Value[] args, Highlight h) {

        highlight(h);

        // Remember the scratch of current expression.
        // scratchStack.push(currentScratch);

        // Create the actor for the invocation.
        int n = 0;
        if (args != null) {
             n = args.length;
        }
        SMIActor actor = factory.produceSMIActor(methodName, n);
        ExpressionActor expr = currentScratch.getExpression(1, -1);
        currentScratch.registerCrap(actor);

        Point invoLoc = expr.getRootLocation();
        actor.setLocation(invoLoc);

        // Create actors and reserve places for all argument values,
        // and create animations to bring them in their right places.
        ValueActor[] argact = new ValueActor[n];
        Animation[] fly = new Animation[n];
        for (int i = 0; i < n; ++i) {
            argact[i] = args[i].getActor();
            args[i].setActor(argact[i]);
            fly[i] = argact[i].fly(actor.reserve(argact[i]));
        }

        // Calculate the size of the invocation actor, taking into account
        // the argument actors.
        actor.calculateSize();

        // Show the animation.
        theatre.capture();

        // Introduce the invocation.
        engine.showAnimation(actor.appear(invoLoc));
        theatre.passivate(actor);

        // Bring in arguments.
        engine.showAnimation(fly);

        // Bind argument actors to the invocation actor.
        for (int i = 0; i < n; ++i) {
            actor.bind(argact[i]);
        }

        return args;
    }

    public void setUpMethod(String methodName, Highlight h) {
        setUpMethod(methodName, null, null, null, h);
    }

    /** Called when the program enters a new user-defined method.
      * Sets up a frame for the method.
      */
    public void setUpMethod(String methodName, Value[] args, String[] formalParameters,
                            String[] formalParameterTypes, Highlight h) {

        // highlight the method header.
        highlight(h);

        // create new method frame
        MethodFrame frame = new MethodFrame(methodName);

        // create a stage for the method
        Stage stage = factory.produceStage(frame);
        frame.setStage(stage);
        currentMethodFrame = frame;
        frameStack.push(frame);

        int n = 0;
        Variable[] vars         = null;
        VariableActor[] varact  = null;
        Animation[] anim        = null;
        ValueActor[] valact     = null;

        if (args != null && args.length > 0) {
            n = args.length;
            vars = new Variable[n];
            varact = new VariableActor[n];
            anim = new Animation[n];
            valact = new ValueActor[n];

            for (int i = 0; i < args.length; ++i) {
                vars[i] = frame.declareVariable(new Variable(formalParameters[i], formalParameterTypes[i]));
                varact[i] = factory.produceVariableActor(vars[i]);
                vars[i].setActor(varact[i]);
                stage.reserve(varact[i]);
                //stage.extend();
                stage.bind();
            }

            Animation a = stage.extend();
            if (a != null) {
                engine.showAnimation(a);
            }
        }

        theatre.capture();
        Point sLoc = manager.reserve(stage);
        engine.showAnimation(stage.appear(sLoc));
        manager.bind(stage);

        if (args != null && args.length > 0) {

            for (int i = 0; i < n; ++i) {
                    vars[i].assign(args[i]);
                    Value casted = vars[i].getValue();
                    ValueActor castact = factory.produceValueActor(casted);
                    valact[i] = args[i].getActor();
                    anim[i] = valact[i].fly(varact[i].reserve(castact));
            }

            engine.showAnimation(anim);

            for (int i = 0; i < n; ++i) {
                varact[i].bind();
                theatre.removeActor(valact[i]);
            }
        }

        if (currentScratch != null) {
            Scratch scratch = currentScratch;
            //scratchStack.push(scratch);
            scratch.memorizeLocation();

            scratch.removeCrap();
            if (eCodeInterpreter.emptyScratch()) {
                scratch.clean();
            }
            manager.removeScratch(scratch);
            Point p = new Point(scratch.getX(), -scratch.getHeight());
            theatre.updateCapture();
            engine.showAnimation(scratch.fly(p));
            theatre.removePassive(scratch);
        }
        openScratch();
        theatre.release();
    }

    public ValueActor finishMethod(Actor returnAct, int expressionCounter) {

        // Get the stage and remove it.
        Stage stage = ((MethodFrame)frameStack.pop()).getStage();
        manager.removeStage(stage);
        Animation stageDisappear = stage.disappear();
        if (returnAct != null) {
            engine.showAnimation(new Animation[] {stageDisappear,
                        returnAct.fly(returnAct.getRootLocation())});
        } else {
            engine.showAnimation(stageDisappear);
        }

        if (!frameStack.empty()) {
            currentMethodFrame = (MethodFrame)frameStack.peek();
        }

        // Remove the current scratch -- the scratch used by the
        // invoked method -- and replace it with the old scratch.
//      manager.removeScratch(currentScratch);
        closeScratch();

        ExpressionActor expr = null;
        if (returnAct != null) {
            expr = currentScratch.findActor(-1);
            if (expr != null) {
                expr.setId(expressionCounter);
            } else {
                expr = currentScratch.getExpression(1, expressionCounter);
            }
        }

        // Get the old location of the scratch
        Point scratchLoc = currentScratch.recallLocation();
        Animation flyScratch = currentScratch.fly(scratchLoc);

        // Create animation to move the old scratch back to its place.
        // If the method returned a value, create another animation to
        // move the return value to the scratch.
        Animation[] anim;
        if (returnAct == null) {

            anim = new Animation[] {flyScratch};
            engine.showAnimation(anim);

        } else {

            theatre.addPassive(currentScratch);
            Point returnLoc = expr.reserve(returnAct);
            returnLoc.translate(
                    scratchLoc.x - currentScratch.getX(),
                    scratchLoc.y - currentScratch.getY());

            Animation flyReturn = returnAct.fly(returnLoc);
            anim = new Animation[] {flyScratch , flyReturn};

            engine.showAnimation(anim);
            expr.bind(returnAct);

        }

        theatre.removeActor(currentScratch);
        manager.addScratch(currentScratch);

        theatre.release();

        if (returnAct != null) {
            return (ValueActor) ((BubbleActor)returnAct).getActor();
        } else {
            return null;
        }
    }

    /** Animates a return statement
      */
    public Actor animateReturn(Value returnValue, Value casted, Highlight h) {

        highlight(h);

        ValueActor castAct = factory.produceValueActor(casted);
        casted.setActor(castAct);
        ValueActor valueAct = returnValue.getActor();

        Stage stage = currentMethodFrame.getStage();
        BubbleActor bubble = factory.produceBubble(stage);

        //The return value goes inside the Method stage in the last
        //place for variables.

        bubble.reserve(castAct);

        Point bubbleLoc = stage.reserve(bubble);
        Animation a = stage.extend();
        if (a != null) {
            engine.showAnimation(a);
        }
        bubble.setLocation(bubbleLoc);

/*      Point bubbleLoc = new Point(
                stage.getX() + stage.getWidth() / 2,
                stage.getY() + stage.getHeight() + 25);
        bubble.setLocation(bubbleLoc);
*/
        Point valueLoc = bubble.reserve(castAct);

        bubble.removeTip();
        theatre.capture();
        engine.showAnimation(bubble.appear(bubbleLoc));
        engine.showAnimation(valueAct.fly(valueLoc));
        bubble.bind();
        //stage.bind();
        theatre.removePassive(valueAct);
        theatre.release();

        return bubble;
    }

    public void animateCastExpression(Value fromValue, Value toValue) {

        ValueActor fromActor = fromValue.getActor();
        ValueActor toActor = factory.produceValueActor(toValue);
        CastActor castActor = new CastActor(fromActor, toActor);
        toValue.setActor(toActor);

        Point loc = fromActor.getRootLocation();
        toActor.setLocation(loc);
        castActor.setLocation(loc);

        ActorContainer parent = fromActor.getParent();
        if (parent != null) {
            parent.removeActor(fromActor);
        }
        theatre.addActor(castActor);
        engine.showAnimation(castActor.cast());
        theatre.removeActor(castActor);
        theatre.addPassive(toActor);
        currentScratch.registerCrap(toActor);
    }

    public Value animateBinaryExpression(
            int operator,
            Value first,
            Value second,
            Value result, int expressionCounter, Highlight h) {

        highlight(h);

        // prepare the actors
        Actor firstAct = first.getActor();

        Actor secondAct = (second == null) ?
                factory.produceEllipsis() :
                second.getActor();

        Actor resultAct = factory.produceValueActor(result);

        OperatorActor operatorAct =
                factory.produceBinOpActor(operator);

        OperatorActor eqAct = factory.produceBinOpResActor(operator);

        ExpressionActor expr = currentScratch.getExpression(5, expressionCounter);
            Point firstLoc      = expr.reserve(firstAct);
            Point operatorLoc   = expr.reserve(operatorAct);
            Point secondLoc     = expr.reserve(secondAct);
            Point eqLoc         = expr.reserve(eqAct);
            Point resultLoc     = expr.reserve(resultAct);

        // Prepare the theatre
        theatre.capture();

        // Move the operands to positions.
        engine.showAnimation(
            new Animation[] {
                firstAct.fly(firstLoc),
                secondAct.fly(secondLoc)
            });

        expr.bind(firstAct);
        expr.bind(secondAct);

        engine.showAnimation(operatorAct.appear(operatorLoc));
        expr.bind(operatorAct);

        engine.showAnimation(eqAct.appear(eqLoc));
        expr.bind(eqAct);

        engine.showAnimation(resultAct.appear(resultLoc));
        expr.bind(resultAct);

        // Darken the expression.
        expr.setLight(Actor.SHADED);

        // Create and set a new actor for the result.
        ValueActor clone = (ValueActor)resultAct.clone();
        result.setActor(clone);
        clone.setLight(Actor.NORMAL);
        clone.setLocation(resultAct.getRootLocation());
        currentScratch.registerCrap(clone);
        theatre.addPassive(clone);

        theatre.release();

        Value val = (Value) result.clone();
        ValueActor rAct = factory.produceValueActor(val);
        rAct.setLight(Actor.NORMAL);
        rAct.setLocation(resultAct.getRootLocation());
        val.setActor(rAct);

        return val;
    }


    public Variable declareVariable(String name, String type, Highlight h) {

        highlight(h);

        // Create a new variable and its actor.
        Variable v = currentMethodFrame.declareVariable(new Variable(name, type));
        VariableActor actor = factory.produceVariableActor(v);
        v.setActor(actor);

        Stage stage = currentMethodFrame.getStage();

        Point loc = stage.reserve(actor);
        Animation a = stage.extend();
        if (a != null) {
            engine.showAnimation(a);
        }
        engine.showAnimation(actor.appear(loc));
        stage.bind();

        return v;
    }

    public void introduceLiteral(Value literal) {
        ValueActor valact = factory.produceValueActor(literal);
        valact.setLocation(cbox.getRootLocation());
        literal.setActor(valact);
    }

    public void introduceInput(Value input) {
        ValueActor valact = factory.produceValueActor(input);
        valact.setLocation(cbox.getRootLocation());
        input.setActor(valact);
    }

    public ValueActor initiateVariableAccess(Variable var) {

        //Problem with instances.
        if (!ECodeUtilities.isPrimitive(var.getType())) {
            return var.getValue().getActor();
        }

//      Value value = var.getValue();
//      Value clone = (Value)value.clone();
//      ValueActor act = factory.produceValueActor(clone);
//      clone.setActor(act);
        ValueActor va = var.getActor().getValue();
        ValueActor act = factory.produceValueActor(va);
        Point loc = va.getRootLocation();

        theatre.capture();
        Animation appear = act.appear(loc);
        appear.setDuration(200);
        engine.showAnimation(appear);
        theatre.release();

        currentScratch.registerCrap(act);
        return act;
    }

    public void animateAssignment(
            Variable variable,
            Value value,
            Value casted,
            Value returnValue, Highlight h) {

        highlight(h);

        String type = variable.getType();
        VariableActor variableAct = variable.getActor();
        ValueActor valueAct = value.getActor();

        if (ECodeUtilities.isPrimitive(type)) {

            // Get/create actors.
            ValueActor castAct = factory.produceValueActor(casted);
            casted.setActor(castAct);

            Point valueLoc = variableAct.reserve(castAct);

            theatre.capture();
            engine.showAnimation(valueAct.fly(valueLoc));
            variableAct.bind();
            theatre.removePassive(valueAct);
            theatre.release();

            if (returnValue != null) {
                ValueActor returnAct =
                        factory.produceValueActor(returnValue);

                returnAct.setLocation(castAct.getRootLocation());
                returnValue.setActor(returnAct);
            }
        }
        else {
            // Get/create actors.
            ReferenceActor refAct = (ReferenceActor) value.getActor();
            //refAct.calculateBends();
            ReferenceVariableActor rva =
                                   (ReferenceVariableActor) variableAct;

            //refAct.setBackground(rva.getBackground());

            ReferenceActor ra = factory.produceReferenceActor(refAct);
            casted.setActor(ra);
            //rva.setReference(refAct);
            //instAct.addReference(refAct);
            Point valueLoc = rva.reserve(ra);

            theatre.capture();
            engine.showAnimation(refAct.fly(valueLoc));
            rva.bind();
            theatre.removePassive(refAct);
            theatre.release();

            if (returnValue != null) {
                ValueActor returnAct =
                  factory.produceReferenceActor((Reference)returnValue);
                returnAct.setLocation(ra.getRootLocation());
                returnValue.setActor(returnAct);
            }

/*
            try {
                Thread.sleep(200);
            }
            catch (InterruptedException e) { }
*/
        }

    }

    public void animatePreIncDec(int operator, Variable var,
            Value result, Highlight h) {

        highlight(h);

        VariableActor varAct = var.getActor();
        //Value value = var.getValue();
        ValueActor resAct = factory.produceValueActor(result);
        ValueActor valAct = var.getActor().getValue();
        //ValueActor valact = factory.produceValueActor(value);
        Actor opAct = factory.produceUnaOpActor(operator);

        Point resLoc = varAct.reserve(valAct);
        Point opLoc = varAct.getRootLocation();

        if (varAct instanceof VariableInArrayActor) {
            opLoc.translate(-15, 8);
        } else {
            opLoc.translate(varAct.getWidth() + 2, 8);
        }

        theatre.capture();

        engine.showAnimation(opAct.appear(opLoc));
        engine.showAnimation(resAct.appear(resLoc));
        varAct.bind();

        //value.setActor(valact);
        result.setActor(resAct);

        //Jeliot 3
        Value val = (Value) result.clone();
        ValueActor rAct = factory.produceValueActor(val);
        rAct.setLight(Actor.NORMAL);
        rAct.setLocation(resAct.getRootLocation());
        val.setActor(rAct);
        //var.assign(val);
        var.getActor().setValue(rAct);

        theatre.removeActor(resAct);
        theatre.removeActor(opAct);
        currentScratch.registerCrap(rAct);

        theatre.release();
    }

    public void animatePostIncDec(int operator, Variable var,
            Value resVal, Highlight h) {

        highlight(h);

        VariableActor varAct = var.getActor();
        //Value value = var.getValue();
        ValueActor valAct = var.getActor().getValue();
        //ValueActor valact = factory.produceValueActor(value);
        ValueActor resAct = (resVal == null) ?
                null :
                factory.produceValueActor(resVal);

        Actor opAct = factory.produceUnaOpActor(operator);

        Point resLoc = varAct.reserve(valAct);
        Point opLoc = varAct.getRootLocation();

        if (varAct instanceof VariableInArrayActor) {
            opLoc.translate(-15, 8);
        } else {
            opLoc.translate(varAct.getWidth() + 2, 8);
        }

        theatre.capture();
        if (resAct != null) {
            Point movLoc = new Point(opLoc);
            movLoc.translate(6, -resAct.getHeight() - 6);
            engine.showAnimation(resAct.appear(resLoc));
            engine.showAnimation(resAct.fly(movLoc));
        }

        engine.showAnimation(opAct.appear(opLoc));
        engine.showAnimation(valAct.appear(resLoc));
        varAct.bind();

        //value.setActor(valact);
        if (resVal != null) {
            resVal.setActor(resAct);
            //var.assign(resval); //jeliot 3
            var.getActor().setValue(resAct);
            currentScratch.registerCrap(resAct);
        }

        theatre.removeActor(opAct);

        theatre.release();
    }

    public ExpressionActor beginUnaryExpression(int operator, Value arg,
                                    int expressionCounter, Highlight h) {
        highlight(h);

        ValueActor argAct = arg.getActor();
        OperatorActor opAct = factory.produceUnaOpActor(operator);

        theatre.capture();

        ExpressionActor exp = currentScratch.getExpression(4, expressionCounter);
        Point oLoc = exp.reserve(opAct);
        Point aLoc = exp.reserve(argAct);

        engine.showAnimation(argAct.fly(aLoc));
        exp.bind(argAct);
        theatre.updateCapture();

        engine.showAnimation(opAct.appear(oLoc));
        exp.bind(opAct);
        theatre.updateCapture();
        theatre.release();

        return exp;
    }

    public Value finishUnaryExpression(int operator, ExpressionActor exp,
                            Value result, int expressionCounter, Highlight h) {

        highlight(h);

        ValueActor resAct = factory.produceValueActor(result);
        OperatorActor eqAct = factory.produceUnaOpResActor(operator);

        theatre.capture();

        Point eLoc = exp.reserve(eqAct);
        Point rLoc = exp.reserve(resAct);

        engine.showAnimation(eqAct.appear(eLoc));
        exp.bind(eqAct);
        theatre.updateCapture();
        engine.showAnimation(resAct.appear(rLoc));
        exp.bind(resAct);
        exp.setLight(Actor.SHADED);

        theatre.release();

        ValueActor clone = (ValueActor)resAct.clone();
        clone.setLight(Actor.NORMAL);
        clone.setLocation(resAct.getRootLocation());
        theatre.addActor(clone);
        currentScratch.registerCrap(clone);
        result.setActor(clone);

        Value val = (Value) result.clone();
        ValueActor rAct = factory.produceValueActor(val);
        rAct.setLight(Actor.NORMAL);
        rAct.setLocation(resAct.getRootLocation());
        val.setActor(rAct);

        return val;
    }

    public Value animateUnaryExpression(int operator, Value arg,
            Value result, int expressionCounter, Highlight h) {

        highlight(h);

        ValueActor argAct = arg.getActor();
        ValueActor resAct = factory.produceValueActor(result);

        OperatorActor opAct = factory.produceUnaOpActor(operator);
        OperatorActor eqAct = factory.produceUnaOpResActor(operator);

        theatre.capture();

        ExpressionActor exp = currentScratch.getExpression(4, expressionCounter);
        Point oLoc = exp.reserve(opAct);
        Point aLoc = exp.reserve(argAct);
        Point eLoc = exp.reserve(eqAct);
        Point rLoc = exp.reserve(resAct);

        engine.showAnimation(argAct.fly(aLoc));
        exp.bind(argAct);
        theatre.updateCapture();

        engine.showAnimation(opAct.appear(oLoc));
        exp.bind(opAct);
        theatre.updateCapture();

        engine.showAnimation(eqAct.appear(eLoc));
        exp.bind(eqAct);
        theatre.updateCapture();
        engine.showAnimation(resAct.appear(rLoc));
        exp.bind(resAct);
        exp.setLight(Actor.SHADED);

        theatre.release();

        ValueActor clone = (ValueActor)resAct.clone();
        clone.setLight(Actor.NORMAL);
        clone.setLocation(resAct.getRootLocation());
        theatre.addActor(clone);
        currentScratch.registerCrap(clone);
        result.setActor(clone);

        Value val = (Value) result.clone();
        val.setActor((ValueActor) clone.clone());
        return val;
    }


    private void showMessage(String[] message) {

        if (jeliot.showMessagesInDialogs()) {

            String msg = "";
            int n = message.length;
            for (int i = 0; i < n; i++) {
                msg += message[i] + "\n";
            }

            JOptionPane.showMessageDialog(null,
                                          msg,
                                          "Message",
                                          JOptionPane.PLAIN_MESSAGE);
        } else {
            MessageActor actor = factory.produceMessageActor(message);
            showMessage(actor);
        }
    }

    private void showMessage(String message) {

        if (jeliot.showMessagesInDialogs()) {
            JOptionPane.showMessageDialog(null,
                                          message,
                                          "Message",
                                          JOptionPane.PLAIN_MESSAGE);
        } else {
            String[] ms = {message};
            MessageActor actor = factory.produceMessageActor(ms);
            showMessage(actor);
        }
    }

    private void showMessage(String message, Value val) {

        if (jeliot.showMessagesInDialogs()) {
            JOptionPane.showMessageDialog(null,
                                          message,
                                          "Message",
                                          JOptionPane.PLAIN_MESSAGE);
        } else {
            String[] ms = {message};
            MessageActor actor = factory.produceMessageActor(ms);
            ValueActor valact = val.getActor();
            showMessage(actor, valact);
        }
    }

    private void showMessage(MessageActor message, Actor anchor) {

        Point aloc = anchor.getRootLocation();
        Dimension asize = anchor.getSize();
        aloc.translate(asize.width/2, 0);
        Dimension msize = message.getSize();
        Dimension tsize = theatre.getSize();

        if (aloc.x - msize.width/2 < 10) {
            aloc.x = 10;
        } else if (aloc.x + msize.width/2 > tsize.width-10) {
            aloc.x = tsize.width-10-msize.width;
        } else {
            aloc.x -= msize.width/2;
        }

        aloc.y += asize.height + 10;
        showMessage(message, new Point(aloc.x, aloc.y));
        currentScratch.registerCrap(anchor);
    }

    private void showMessage(MessageActor message) {
        Dimension msize = message.getSize();
        Dimension tsize = theatre.getSize();
        int x = (tsize.width-msize.width)/2;
        int y = (tsize.height-msize.height)/2;
        showMessage(message, new Point(x, y));
    }

    private void showMessage(MessageActor message, Point p) {
        theatre.capture();
        engine.showAnimation(message.appear(p));
        theatre.removeActor(message);
        theatre.release();
    }

    public void openScope() {
        highlight(null);
        showMessage("Opening new scope for variables.");
        getCurrentMethodFrame().openScope();
    }

    public void closeScope() {
        highlight(null);
        showMessage("Closing a scope and erasing the scope variables.");
        getCurrentMethodFrame().closeScope();
    }

    public void enterLoop(String statementName, Highlight h) {
        highlight(h);
        showMessage("Entering the " + statementName + " loop.");
    }

    public void enterLoop(String statementName, Value check, Highlight h) {
        highlight(h);
        showMessage("Entering the " + statementName + " loop.", check);
    }

    public void continueLoop(String statementName, Value check, Highlight h) {
        highlight(h);
        showMessage("Continuing the " + statementName + " loop in the next round.", check);
    }

    public void exitLoop(String statementName, Value check) {
        highlight(null);
        showMessage("Exiting the " + statementName + " loop.", check);
    }

    public void breakLoop(String statementName, Highlight h) {
        highlight(h);
        showMessage("Exiting the loop because of break.");
    }

    public void skipLoop(String statementName, Value check) {
        highlight(null);
        showMessage("Not entering the " + statementName + " loop.", check);
    }

    public void continueLoop(String statementName, Highlight h) {
        highlight(h);
        showMessage("Continuing the " + statementName + " loop in the next round.");
    }

    public void branchThen(Value check, Highlight h) {
        highlight(h);
        showMessage("Choosing then-branch.", check);
    }

    public void branchElse(Value check, Highlight h) {
        highlight(h);
        showMessage("Choosing else-branch.", check);
    }

    public void skipIf(Value check, Highlight h) {
        highlight(h);
        showMessage("Continuing without branching.", check);
    }

    public void arrayCreation(int[] dims) {
        String dimensions = "";
        int n = dims.length;
        for (int i = 0; i < n; i++) {
            if (i == n - 1) {
                dimensions += dims[i];
            } else {
                dimensions += dims[i] + ", ";
            }
        }

        showMessage(new String[] {"A " + n +
        "-dimensional array is created.",
        "The dimensions of the array are: " + dimensions + "."});
    }


    public void output(Value val, Highlight h)  {

        highlight(h);

        ValueActor actor = val.getActor();
        if (actor == null) {
            actor = factory.produceValueActor(val);
        }

        AnimatingActor hand = factory.produceHand();

        Point dest = manager.getOutputPoint();
        Point handp = new Point(dest.x, dest.y - hand.getHeight());
        Point vdp = new Point(dest.x + hand.getWidth()/3,
                              dest.y - hand.getHeight()*2/3);

        Animation fist = hand.changeImage(factory.produceImage("Fist-1"));
        fist.setDuration(800);

        theatre.capture();

        hand.setLocation(dest);
        engine.showAnimation(new Animation[] {
                            actor.fly(vdp),
                            hand.fly(handp, 0),
                            fist
                            });

        hand.setImage(factory.produceImage("Fist-2"));
        theatre.removeActor(actor);
        engine.showAnimation(hand.fly(dest,0));

        theatre.removeActor(hand);
        theatre.release();

        this.output(val.getValue() + "\n");
    }

    public void showErrorMessage(InterpreterError e) {
        jeliot.showErrorMessage(e);
    }

    public void output(String str) {
        jeliot.output(str);
    }

    public AnimationEngine getEngine() {
        return engine;
    }

    public TheatreManager getManager() {
        return manager;
    }

    public ActorFactory getFactory() {
        return factory;
    }
    public Theatre getTheatre() {
        return theatre;
    }

    public static Animator readInt() {
        return new InputAnimator(
            "Please input an integer.",
            new InputValidator() {
                public void validate(String s) {
                    try {
                        Integer i = new Integer(s);
                        accept(new Value(i.toString(), int.class.getName()));
                    }
                    catch (NumberFormatException e) { }
                }
            }
        );
    }

    public static Animator readDouble() {
        return new InputAnimator(
            "Please input a double.",
            new InputValidator() {
                public void validate(String s) {
                    try {
                        Double d = new Double(s);
                        accept(new Value(d.toString(), double.class.getName()));
                    }
                    catch (NumberFormatException e) { }
                }
            }
        );
    }

    public static Animator readString() {
        return new InputAnimator(
            "Please input a string.",
            new InputValidator() {
                public void validate(String s) {
                    try {
                        accept(new Value(s, "".getClass().getName()));
                    }
                    catch (NumberFormatException e) { }
                }
            }
        );
    }

    public static Animator readChar() {
        return new InputAnimator(
            "Please input a character.",
            new InputValidator() {
                public void validate(String s) {
                    if (s.length() == 1) {
                        accept(new Value(s, char.class.getName()));
                    }
                }
            }
        );
    }

    //possibly we will need some other input readers e.g. readString(), readChar()
    private static class InputAnimator extends Animator {

        private String prompt;
        private InputValidator validator;

        private InputAnimator(String prompt, InputValidator valid) {

            this.prompt = prompt;
            this.validator = valid;
        }

        public void animate(Director director) {
            Value val = director.getInput(prompt, validator);
            setReturnValue(val);
        }
    }

    /**
     * Shows an animation of the invocation of a static foreign
     * method.
     */
    public Value animateInputHandling(String type, Highlight h) {

        Animator animator = null;

        if (ECodeUtilities.resolveType(type) == ECodeUtilities.DOUBLE) {
            animator = readDouble();
        } else if (ECodeUtilities.resolveType(type) == ECodeUtilities.INT) {
            animator = readInt();
        } else if (ECodeUtilities.resolveType(type) == ECodeUtilities.STRING) {
            animator = readString();
        } else if (ECodeUtilities.resolveType(type) == ECodeUtilities.CHAR) {
            animator = readChar();
        }

        if (animator != null) {
            highlight(h);
            animator.animate(this);
            return animator.getReturnValue();
        } else {
            return null;
        }
    }

    public Value getInput(String prompt, InputValidator validator) {

        validator.setController(controller);
        final InputComponent ic = new InputComponent(prompt, validator);
        final ExpressionActor ea = currentScratch.getExpression(1,-2);
        Actor bga = factory.produceMessageActor(null);
        final Point p = ea.reserve(bga);
        ic.setBgactor(bga);

        try {
            SwingUtilities.invokeAndWait(
                new Runnable() {
                    public void run() {
                        theatre.add(ic);
                        ic.setSize(ic.getPreferredSize());
                        ic.setLocation(p);
                        ic.revalidate();
                        theatre.showComponents(true);
                        theatre.release();
                        ic.popup();
                        theatre.repaint();
                    }
                }
            );
        }
        catch (java.lang.reflect.InvocationTargetException e) {
            e.printStackTrace();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }

        do {
            controller.pause();

            controller.checkPoint(
                new Controlled() {

                    public void suspend() {
                        jeliot.directorFreezed();
                    }

                    public void resume() {
                        jeliot.directorResumed();
                    }
                }
            );
        } while (!validator.isOk());

        try {
            SwingUtilities.invokeAndWait(
                new Runnable() {
                    public void run() {
                        theatre.remove(ic);
                        theatre.showComponents(false);
                        theatre.repaint();
                    }
                }
            );
        }
        catch (java.lang.reflect.InvocationTargetException e) {
            e.printStackTrace();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }

        Value val = validator.getValue();
        ValueActor act = factory.produceValueActor(val);
        act.setLocation(p);
        val.setActor(act);
        currentScratch.registerCrap(act);
        theatre.addActor(act);

        return val;
    }

    public void showArrayCreation(ArrayInstance array, jeliot.lang.Reference ref,
                                  Value[] lenVal, int expressionCounter,
                                  Highlight h) {

        highlight(h);

        //Array creation here
        //Use SMIActor as base.

        ArrayActor arrayAct = factory.produceArrayActor(array);
        array.setActor(arrayAct);

        Point loc = manager.reserve(arrayAct);
        theatre.capture();
        engine.showAnimation(arrayAct.appear(loc));
        theatre.release();
        manager.bind(arrayAct);

        ReferenceActor refAct = factory.produceReferenceActor(ref);
        ref.setActor(refAct);
        refAct.setLocation(arrayAct.getRootLocation());

        theatre.capture();

        ExpressionActor expr = currentScratch.getExpression(1, expressionCounter);
        Point firstLoc = expr.reserve(refAct);
        engine.showAnimation(refAct.fly(firstLoc));
        expr.bind(refAct);

        theatre.release();

    }

    public void showArrayAccess(VariableInArray var,
                                Value[] indexVal,
                                Value returnVal,
                                Highlight h) {

        highlight(h);

        Value value = var.getValue();

        final VariableInArrayActor varAct = (VariableInArrayActor)var.getActor();

        ValueActor returnAct = factory.produceValueActor(returnVal);
        returnVal.setActor(returnAct);
        Point loc = value.getActor().getRootLocation();
        returnAct.setLocation(loc);

        int n = indexVal.length;

        Animation[] appear = new Animation[n];
        Animation[] index = new Animation[n];
        IndexActor[] indexAct = new IndexActor[n];
        ValueActor[] indexValAct = new ValueActor[n];

        for (int i = 0; i < n; i++) {
            indexValAct[i] = indexVal[i].getActor();
            indexAct[i] = new IndexActor(indexValAct[i]);
            appear[i] = indexValAct[i].appear(indexValAct[i].getRootLocation());
            appear[i].setDuration(600);
            index[i] = indexAct[i].index(varAct);
        }

        theatre.capture();
        engine.showAnimation(appear);
        engine.showAnimation(index);
        varAct.setLight(Actor.HIGHLIGHT);
        //engine.showAnimation(returnAct.appear(loc));
        theatre.release();

        for (int i = 0; i < n; i++) {
            currentScratch.registerCrap(indexValAct[i]);
            currentScratch.registerCrap(indexAct[i]);
        }
        //currentScratch.registerCrap(returnAct);

        currentScratch.registerCrapRemover(
            new Runnable() {
                public void run() {
                    varAct.setLight(Actor.NORMAL);
                }
            }
        );
    }

/*
    public void showArrayVariableAccess(VariableInArray var,
                                        Value indexVal) {

        //System.err.println("Wopee! " + var + " " + indexVal);

        final VariableInArrayActor varAct =
                (VariableInArrayActor)var.getActor();

        ValueActor indexValAct = indexVal.getActor();

        IndexActor indexAct = new IndexActor(indexValAct);
        Animation appear = indexValAct.appear(
                indexValAct.getRootLocation());
        appear.setDuration(600);

        theatre.capture();
        engine.showAnimation(appear);
        engine.showAnimation(indexAct.index(varAct));
        varAct.setLight(Actor.HIGHLIGHT);
        theatre.release();

        currentScratch.registerCrap(indexValAct);
        currentScratch.registerCrap(indexAct);
        currentScratch.registerCrapRemover(
            new Runnable() {
                public void run() {
                    varAct.setLight(Actor.NORMAL);
                }
            }
        );

    }
*/
    //Jeliot 3
    //Used for setting the reader for ecode interpreter
    public void initializeInterpreter(BufferedReader br,
                                      String programCode,
                                      PrintWriter pr) {
        this.eCodeInterpreter = new Interpreter(br, this, programCode, pr);
        eCodeInterpreter.initialize();
    }
}