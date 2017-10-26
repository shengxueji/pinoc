package com.iqiyi.trojan.plugin;

import org.gradle.internal.impldep.aQute.bnd.osgi.resource.FilterParser;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ByteVector;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.commons.AdviceAdapter;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by zhaolifei on 2017/8/21.
 */

class TrojanPluginMethodVisitor extends AdviceAdapter {

    private ArrayList<String> parameterTypes;

    private String returnType;

    private String className;

    private String methodName;

    private String methodSignature;

    private static final HashMap<Character, String> PRIMITIVE_SIGNATURES = new HashMap<Character, String>() {
        {
            put('Z', "boolean");
            put('B', "byte");
            put('C', "char");
            put('S', "short");
            put('I', "int");
            put('J', "long");
            put('F', "float");
            put('D', "double");
            put('V', "void");
        }
    };

    private static final HashMap<String, Character> PRIMITIVE_SIGNATURES_SHORT_FORMS = new HashMap<String, Character>() {
        {
            put("boolean", 'Z');
            put("byte", 'B');
            put("char", 'C');
            put("short", 'S');
            put("int", 'I');
            put("long", 'J');
            put("float", 'F');
            put("double", 'D');
            put("void", 'V');
        }
    };

    private static final HashMap<String, String> PRIMITIVE_CLASSES = new HashMap<String, String>() {
        {
            put("boolean", "java/lang/Boolean");
            put("byte", "java/lang/Byte");
            put("char", "java/lang/Character");
            put("short", "java/lang/Short");
            put("int", "java/lang/Integer");
            put("long", "java/lang/Long");
            put("float", "java/lang/Float");
            put("double", "java/lang/Double");
            put("void", "java/lang/Void");
        }
    };

    private static final HashMap<String, Integer> PRIMITIVE_LOADS = new HashMap<String, Integer>() {
        {
            put("boolean", Opcodes.ILOAD);
            put("byte", Opcodes.ILOAD);
            put("char", Opcodes.ILOAD);
            put("short", Opcodes.ILOAD);
            put("int", Opcodes.ILOAD);
            put("long", Opcodes.LLOAD);
            put("float", Opcodes.FLOAD);
            put("double", Opcodes.DLOAD);
        }
    };

    private static final HashMap<String, Integer> PRIMITIVE_RETURNS = new HashMap<String, Integer>() {
        {
            put("boolean", Opcodes.IRETURN);
            put("byte", Opcodes.IRETURN);
            put("char", Opcodes.IRETURN);
            put("short", Opcodes.IRETURN);
            put("int", Opcodes.IRETURN);
            put("long", Opcodes.LRETURN);
            put("float", Opcodes.FRETURN);
            put("double", Opcodes.DRETURN);
        }
    };

    private static final HashMap<String, String> PRIMITIVE_VALUE_OF_SIGNATURES = new HashMap<String, String>() {
        {
            put("boolean", "(Z)Ljava/lang/Boolean;");
            put("byte", "(B)Ljava/lang/byte;");
            put("char", "(C)Ljava/lang/Character;");
            put("short", "(S)Ljava/lang/Short;");
            put("int", "(I)Ljava/lang/Integer;");
            put("long", "(J)Ljava/lang/Long;");
            put("float", "(F)Ljava/lang/Float;");
            put("double", "(D)Ljava/lang/Double;");
        }
    };

    private static final HashMap<String, Integer> PRIMITIVE_FRAMES = new HashMap<String, Integer>() {
        {
            put("boolean", Opcodes.INTEGER);
            put("byte", Opcodes.INTEGER);
            put("char", Opcodes.INTEGER);
            put("short", Opcodes.INTEGER);
            put("int", Opcodes.INTEGER);
            put("long", Opcodes.LONG);
            put("float", Opcodes.FLOAT);
            put("double", Opcodes.DOUBLE);
        }
    };

    TrojanPluginMethodVisitor(int api, MethodVisitor mv, int access, String className, String methodName, String desc) {
        super(api, new TestMethodVisitor2(api, mv), access, methodName, desc);
        initParameterTypes(desc);
        this.className = className;
        this.methodName = methodName;
        this.methodSignature = desc;
    }

    private Object[] obtainType(String desc, int start) {
        char ch = desc.charAt(start);
        String sig = PRIMITIVE_SIGNATURES.get(ch);
        if (sig != null) {
            return new Object[]{start + 1, sig};
        }
        if (ch == 'L') {
            int pos = start + 1;
            while (desc.charAt(pos) != ';') {
                ++pos;
            }
            return new Object[]{pos + 1, desc.substring(start + 1, pos)};
        }
        if (ch == '[') {
            Object[] tmp = obtainType(desc, start + 1);
            return new Object[]{tmp[0], tmp[1] + "[]"};
        }
        throw new IllegalArgumentException("What the fuck!");
    }

    private void initParameterTypes(String desc) {
        parameterTypes = new ArrayList<>();
        int pos = 1;
        while (desc.charAt(pos) != ')') {
            Object[] tmp = obtainType(desc, pos);
            parameterTypes.add((String) tmp[1]);
            pos = (int) tmp[0];
        }
        ++pos;
        returnType = (String) obtainType(desc, pos)[1];
        System.out.println("return type = " + returnType);
    }

    private void pushConst(int number) {
        switch (number) {
            case 0:
                mv.visitInsn(Opcodes.ICONST_0);
                break;
            case 1:
                mv.visitInsn(Opcodes.ICONST_1);
                break;
            case 2:
                mv.visitInsn(Opcodes.ICONST_2);
                break;
            case 3:
                mv.visitInsn(Opcodes.ICONST_3);
                break;
            case 4:
                mv.visitInsn(Opcodes.ICONST_4);
                break;
            case 5:
                mv.visitInsn(Opcodes.ICONST_5);
                break;
            default:
                mv.visitIntInsn(Opcodes.BIPUSH, number);
                break;
        }
    }

//    private Object[] toArray(ArrayList<Object> list) {
//        int length = list.size();
//        Object[] result = new Object[length];
//        for (int i = 0; i < length; ++i) {
//            result[i] = list.get(i);
//        }
//        return result;
//    }

    @Override
    protected void onMethodEnter() {
        System.out.println("onMethodEnter");
        mv.visitLdcInsn(className);
        mv.visitLdcInsn(methodName);
        mv.visitLdcInsn(methodSignature);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        int parameterNumber = parameterTypes.size();
        pushConst(parameterNumber);
        mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
        int offset = 1;
        for (int i = 0; i < parameterNumber; ++i) {
            mv.visitInsn(Opcodes.DUP);
            pushConst(i);
            String parameterType = parameterTypes.get(i);
            String className = PRIMITIVE_CLASSES.get(parameterType);
            if (className != null) {
                mv.visitVarInsn(PRIMITIVE_LOADS.get(parameterType), offset++);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                        PRIMITIVE_CLASSES.get(parameterType),
                        "valueOf",
                        PRIMITIVE_VALUE_OF_SIGNATURES.get(parameterType),
                        false);
                if (parameterType.equals("long") || parameterType.equals("double")) {
                    ++offset;
                }
            } else {
                mv.visitVarInsn(Opcodes.ALOAD, offset++);
            }
            mv.visitInsn(Opcodes.AASTORE);
        }
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "com/iqiyi/trojan/Trojan",
                "onEnterMethod",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;",
                false);
        mv.visitVarInsn(Opcodes.ASTORE, offset);
        mv.visitVarInsn(Opcodes.ALOAD, offset);
        mv.visitFieldInsn(Opcodes.GETSTATIC, "xiaofei/library/zlang/Library", "NO_RETURN_VALUE", "Ljava/lang/Object;");
        Label label = new Label();
        mv.visitJumpInsn(Opcodes.IF_ACMPEQ, label);
        if (returnType.equals("void")) {
            mv.visitInsn(Opcodes.RETURN);
        } else if (returnType.equals("java/lang/Void")) {
            mv.visitInsn(Opcodes.ACONST_NULL);
            mv.visitInsn(Opcodes.ARETURN);
        } else {
            String tmpReturnType = PRIMITIVE_CLASSES.get(returnType);
            if (tmpReturnType == null) {
                Label label1 = new Label();
                mv.visitVarInsn(Opcodes.ALOAD, offset);
                mv.visitJumpInsn(Opcodes.IFNONNULL, label1);
                mv.visitInsn(Opcodes.ACONST_NULL);
                mv.visitInsn(Opcodes.ARETURN);
                mv.visitLabel(label1);
                Object[] objects = new Object[parameterNumber + 2];
                objects[0] = className;
//                ArrayList<Object> objects = new ArrayList<>();
//                objects.add(className);
                for (int i = 1; i <= parameterNumber; ++i) {
                    String parameterType = parameterTypes.get(i - 1);
                    Integer frame = PRIMITIVE_FRAMES.get(parameterType);
                    if (frame != null) {
//                        objects.add(frame);
//                        if (frame.equals(Opcodes.LONG) || frame.equals(Opcodes.DOUBLE)) {
//                            objects.add(frame);
//                        }
                        objects[i] = frame;
                    } else {
//                        objects.add(parameterNumber);
                        objects[i] = parameterType;
                    }
                }
//                objects.add("java/lang/Object");
                objects[parameterNumber + 1] = "java/lang/Object";
                mv.visitFrame(Opcodes.F_NEW, parameterNumber + 2, objects, 0, new Object[0]);
            }
            mv.visitVarInsn(Opcodes.ALOAD, offset);
            if (tmpReturnType == null) {
                mv.visitTypeInsn(Opcodes.INSTANCEOF, returnType);
                mv.visitJumpInsn(Opcodes.IFEQ, label);
                mv.visitVarInsn(Opcodes.ALOAD, offset);
                mv.visitTypeInsn(Opcodes.CHECKCAST, returnType);
                mv.visitInsn(Opcodes.ARETURN);
            } else {
                mv.visitTypeInsn(Opcodes.INSTANCEOF, tmpReturnType);
                mv.visitJumpInsn(Opcodes.IFEQ, label);
                mv.visitVarInsn(Opcodes.ALOAD, offset);
                mv.visitTypeInsn(Opcodes.CHECKCAST, tmpReturnType);
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                        tmpReturnType,
                        returnType + "Value",
                        "()" + PRIMITIVE_SIGNATURES_SHORT_FORMS.get(returnType),
                        false);
                mv.visitInsn(PRIMITIVE_RETURNS.get(returnType));
            }
        }
        mv.visitLabel(label);
//        ArrayList<Object> objects = new ArrayList<>();
//        objects.add(className);
        Object[] objects = new Object[parameterNumber + 2];
        objects[0] = className;
        for (int i = 1; i <= parameterNumber; ++i) {
            String parameterType = parameterTypes.get(i - 1);
            Integer frame = PRIMITIVE_FRAMES.get(parameterType);
            if (frame != null) {
//                objects.add(frame);
//                if (frame.equals(Opcodes.LONG) || frame.equals(Opcodes.DOUBLE)) {
//                    objects.add(frame);
//                }
                objects[i] = frame;
            } else {
//                objects.add(parameterNumber);
                objects[i] = parameterType;
            }
        }
//        objects.add("java/lang/Object");
//        mv.visitFrame(Opcodes.F_NEW, parameterNumber + 2, toArray(objects), 0, new Object[0]);
        objects[parameterNumber + 1] = "java/lang/Object";
        mv.visitFrame(Opcodes.F_NEW, parameterNumber + 2, objects, 0, new Object[0]);
    }

    @Override
    public void visitLabel(Label label) {
        System.out.println("visitLabel " + label);
        super.visitLabel(label);
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        System.out.println("visit var insn1 " + opcode +  " " + var + " " + parameterTypes.size());
        if (var <= parameterTypes.size()) {
            System.out.println("visit var insn2 " + opcode +  " " + var);
            mv.visitVarInsn(opcode, var);
        } else {
            System.out.println("visit var insn3 " + opcode +  " " + (var + 1));
            mv.visitVarInsn(opcode, var + 1);
        }
    }

    @Override
    public void visitIincInsn(int var, int increment) {
        System.out.println("visit iinc insn " + var +  " " + increment);
        if (var <= parameterTypes.size()) {
            mv.visitIincInsn(var, increment);
        } else {
            mv.visitIincInsn(var + 1, increment);
        }
    }

    @Override
    public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
        System.out.println("visitLocalVariable1 " + name +  " " + desc + " " + signature + " " + start + " " + end + "  " + index);
        int size = parameterTypes.size();
        if (index <= size) {
            System.out.println("visitLocalVariable2 " + name +  " " + desc + " " + signature + " " + start + " " + end + "  " + index);
            mv.visitLocalVariable(name, desc, signature, start, end, index);
        } else {
            System.out.println("visitLocalVariable3 " + name +  " " + desc + " " + signature + " " + start + " " + end + "  " + (index + 1));
            mv.visitLocalVariable(name, desc, signature, start, end, index + 1);
        }
    }

    @Override
    public void visitFrame(int i, int i1, Object[] objects, int i2, Object[] objects1) {
        System.out.println("Frame " + i + " " + i1 + " " + i2);
        for (int j = 0; j < objects.length; ++j) {
            System.out.print(" " + objects[j]);
        }
        System.out.println();
        for (int j = 0; j < objects1.length; ++j) {
            System.out.print(" " + objects1[j]);
        }
        System.out.println();
        super.visitFrame(i, i1, objects, i2, objects1);
    }


    @Override
    public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath, Label[] start, Label[] end, int[] index, String desc, boolean visible) {
        System.out.println("visitLocalVariableAnnotation");
        return super.visitLocalVariableAnnotation(typeRef, typePath, start, end, index, desc, visible);
    }

    @Override
    public void visitMaxs(int i, int i1) {
        super.visitMaxs(0, 0);
    }
}
