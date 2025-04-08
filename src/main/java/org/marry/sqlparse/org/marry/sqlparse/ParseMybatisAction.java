package org.marry.sqlparse.org.marry.sqlparse;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.tree.java.PsiLiteralExpressionImpl;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.session.Configuration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author mal
 * @date 2025/4/8 10:50
 */
public class ParseMybatisAction extends AnAction {

    private static final Set<String> statementAnnotationTypes = Stream.of("org.apache.ibatis.annotations.Select", "org.apache.ibatis.annotations.Update", "org.apache.ibatis.annotations.Insert", "org.apache.ibatis.annotations.Delete").collect(Collectors.toSet());


    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return super.getActionUpdateThread();
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        // 仅在选中Java方法时启用
        @Nullable PsiElement element = e.getData(LangDataKeys.PSI_ELEMENT);
        e.getPresentation().setEnabledAndVisible(element instanceof PsiMethod);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        PsiMethod method = (PsiMethod) e.getData(LangDataKeys.PSI_ELEMENT);
        if (method == null) return;

        PsiClass containingClass = method.getContainingClass();
        if (containingClass == null) return;

        String sqlContent = buildAnnotationInfo(method);

        String emptySql = createFakeSql(sqlContent);

        showAnnotationsWithCopy(e.getProject(), emptySql);
    }

    private String createFakeSql(String sqlContent) {

        if (sqlContent.length() == 0) {
            return "No Sql";
        }

        String sql = null;
        try {
            SqlSource sqlSource = new XMLLanguageDriver().createSqlSource(new Configuration(), sqlContent, Map.class);
            BoundSql boundSql = sqlSource.getBoundSql(new HashMap<>());
            sql = boundSql.getSql();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("解析sql失败");
            return sqlContent;
        }
        return sql;
    }

    private String buildAnnotationInfo(PsiMethod method) {
        PsiAnnotation[] annotations = method.getAnnotations();
        StringBuilder sb = new StringBuilder();

        if (annotations.length == 0) {
            return "No annotations";
        }
        Optional<PsiAnnotation> sqlAnnotation = Arrays.stream(annotations).filter(a -> statementAnnotationTypes.contains(a.getQualifiedName())).findFirst();
        if (sqlAnnotation.isPresent()) {
            PsiAnnotation psiAnnotation = sqlAnnotation.get();
            PsiAnnotationParameterList parameterList = psiAnnotation.getParameterList();
            for (PsiNameValuePair attribute : parameterList.getAttributes()) {
                PsiAnnotationMemberValue value = attribute.getValue();
                if (value != null) {
                    @NotNull PsiElement[] children = value.getChildren();
                    for (@NotNull PsiElement child : children) {
                        if (child instanceof PsiLiteralExpressionImpl) {
                            sb.append(((PsiLiteralExpressionImpl) child).getValue());
                            sb.append("\n");
                        }
                    }
                    break;
                }
            }
        }


        return sb.toString();
    }

    private String buildAnnotationInfo(PsiClass psiClass) {
        StringBuilder sb = new StringBuilder();
        PsiAnnotation[] annotations = psiClass.getAnnotations();

        if (annotations.length == 0) {
            return "No annotations found on class";
        }

        sb.append("Class Annotations (").append(annotations.length).append("):\n\n");
        for (PsiAnnotation annotation : annotations) {
            sb.append(formatAnnotation(annotation)).append("\n\n");
        }
        return sb.toString().trim();
    }

    private String formatAnnotation(PsiAnnotation annotation) {
        String annotationName = annotation.getQualifiedName();
        PsiAnnotationParameterList params = annotation.getParameterList();

        // 处理注解参数
        StringBuilder paramBuilder = new StringBuilder();
        for (PsiNameValuePair pair : params.getAttributes()) {
            String value = pair.getLiteralValue();
            if (value != null) {
                paramBuilder.append(pair.getName()).append(" = ").append(value).append(", ");
            }
        }

        String parameters = paramBuilder.length() > 0 ?
                paramBuilder.substring(0, paramBuilder.length() - 2) :
                "[no parameters]";

        return "@" + annotationName + "(" + parameters + ")";
    }

    private void showAnnotations(Project project, String content) {
        // 使用更大的对话框显示
        JTextArea textArea = new JTextArea(content);
        textArea.setEditable(false);
        textArea.setWrapStyleWord(true);
        textArea.setLineWrap(true);

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(500, 300));

        JOptionPane.showMessageDialog(
                null,
                scrollPane,
                "Class Annotations",
                JOptionPane.INFORMATION_MESSAGE
        );

    }

    // 示例：添加复制到剪贴板功能（在showAnnotations方法中添加按钮）
    private void showAnnotationsWithCopy(Project project, String content) {
        JPanel panel = new JPanel(new BorderLayout());

        JTextArea textArea = new JTextArea(content);
        textArea.setEditable(false);
        panel.add(new JScrollPane(textArea), BorderLayout.CENTER);

        JButton copyButton = new JButton("Copy to Clipboard");
        copyButton.addActionListener(e -> {
            StringSelection selection = new StringSelection(content);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(selection, null);
        });
        panel.add(copyButton, BorderLayout.SOUTH);

        JOptionPane.showMessageDialog(null, panel, "Pure SQL *YoYo*", JOptionPane.PLAIN_MESSAGE);
    }

}
