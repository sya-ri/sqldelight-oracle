package dev.s7a.sqldelight.oracle.dialects.oracle.grammar.psi.impl;

import com.alecstrong.sql.psi.core.SqlAnnotationHolder;
import com.alecstrong.sql.psi.core.SqlFileBase;
import com.alecstrong.sql.psi.core.psi.LazyQuery;
import com.alecstrong.sql.psi.core.psi.QueryElement.QueryResult;
import com.alecstrong.sql.psi.core.psi.Schema;
import com.alecstrong.sql.psi.core.psi.SchemaContributor;
import com.alecstrong.sql.psi.core.psi.SchemaContributorStub;
import com.alecstrong.sql.psi.core.psi.SqlColumnDef;
import com.alecstrong.sql.psi.core.psi.SqlCompositeElement;
import com.alecstrong.sql.psi.core.psi.SqlCompoundSelectStmt;
import com.alecstrong.sql.psi.core.psi.SqlCreateTableStmt;
import com.alecstrong.sql.psi.core.psi.SqlDatabaseName;
import com.alecstrong.sql.psi.core.psi.SqlTableConstraint;
import com.alecstrong.sql.psi.core.psi.SqlTableName;
import com.alecstrong.sql.psi.core.psi.SqlTableOptions;
import com.alecstrong.sql.psi.core.psi.TableElement;
import com.intellij.extapi.psi.StubBasedPsiElementBase;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import kotlin.jvm.internal.Reflection;
import kotlin.reflect.KClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

abstract class OracleCreateSynonymMixin extends StubBasedPsiElementBase<SchemaContributorStub>
    implements TableElement, SqlCreateTableStmt {
  OracleCreateSynonymMixin(@NotNull ASTNode node) {
    this(null, null, node);
  }

  OracleCreateSynonymMixin(@NotNull SchemaContributorStub stub, @NotNull IStubElementType<?, ?> nodeType) {
    this(stub, nodeType, null);
  }

  OracleCreateSynonymMixin(
      @Nullable SchemaContributorStub stub,
      @Nullable IElementType nodeType,
      @Nullable ASTNode node
  ) {
    super(stub, nodeType, node);
  }

  @NotNull
  @Override
  public Collection<QueryResult> queryAvailable(@NotNull PsiElement child) {
    SqlTableName tableName = tableName();
    if (child == tableName || PsiTreeUtil.isAncestor(tableName, child, false)) {
      return Collections.singletonList(new QueryResult(tableName, List.of(), List.of(), null, false));
    }
    return ((SqlCompositeElement) getParent()).queryAvailable(this);
  }

  @NotNull
  @Override
  public Collection<LazyQuery> tablesAvailable(@NotNull PsiElement child) {
    return ((SqlCompositeElement) getParent()).tablesAvailable(this);
  }

  @Override
  public void annotate(@NotNull SqlAnnotationHolder annotationHolder) {
  }

  @NotNull
  @Override
  public SqlFileBase getContainingFile() {
    return (SqlFileBase) super.getContainingFile();
  }

  @NotNull
  @Override
  public String name() {
    SchemaContributorStub stub = getStub();
    return stub != null ? stub.name() : tableName().getName();
  }

  @Override
  public void modifySchema(@NotNull Schema schema) {
    valuesByType(schema, TableElement.class).putIfAbsent(name(), this);
  }

  @NotNull
  @Override
  public LazyQuery tableExposed() {
    SqlTableName tableName = tableName();
    return new LazyQuery(tableName, () -> new QueryResult(tableName, List.of(), List.of(), null, false));
  }

  @NotNull
  @Override
  public List<SqlColumnDef> getColumnDefList() {
    return List.of();
  }

  @Nullable
  @Override
  public SqlCompoundSelectStmt getCompoundSelectStmt() {
    return null;
  }

  @Nullable
  @Override
  public SqlDatabaseName getDatabaseName() {
    return null;
  }

  @NotNull
  @Override
  public List<SqlTableConstraint> getTableConstraintList() {
    return List.of();
  }

  @NotNull
  @Override
  public SqlTableName getTableName() {
    return tableName();
  }

  @Nullable
  @Override
  public SqlTableOptions getTableOptions() {
    return null;
  }

  @NotNull
  private SqlTableName tableName() {
    SqlTableName tableName = PsiTreeUtil.getChildOfType(this, SqlTableName.class);
    if (tableName == null) {
      throw new IllegalStateException("Synonym name is missing");
    }
    return tableName;
  }

  @SuppressWarnings("unchecked")
  private static <Value extends SchemaContributor> Map<String, Value> valuesByType(
      Schema schema,
      Class<Value> type
  ) {
    try {
      Field mapField = Schema.class.getDeclaredField("map");
      mapField.setAccessible(true);
      Map<KClass<? extends SchemaContributor>, Map<String, ? extends SchemaContributor>> schemaMap =
          (Map<KClass<? extends SchemaContributor>, Map<String, ? extends SchemaContributor>>) mapField.get(schema);
      KClass<? extends SchemaContributor> key =
          (KClass<? extends SchemaContributor>) Reflection.getOrCreateKotlinClass(type);
      return (Map<String, Value>) schemaMap.computeIfAbsent(key, ignored -> new LinkedHashMap<>());
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Unable to access SQLDelight schema map", e);
    }
  }
}
