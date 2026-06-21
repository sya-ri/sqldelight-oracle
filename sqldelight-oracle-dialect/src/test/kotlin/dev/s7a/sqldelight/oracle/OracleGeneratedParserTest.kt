package dev.s7a.sqldelight.oracle

import com.alecstrong.sql.psi.core.SqlParserUtil
import dev.s7a.sqldelight.oracle.grammar.OracleParser
import dev.s7a.sqldelight.oracle.grammar.OracleParserUtil
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class OracleGeneratedParserTest :
    FunSpec({
        test("generates Oracle parser classes in the SQLDelight dialect package exactly") {
            OracleParser::class.java.name shouldBe "dev.s7a.sqldelight.oracle.grammar.OracleParser"
            OracleParserUtil::class.java.name shouldBe "dev.s7a.sqldelight.oracle.grammar.OracleParserUtil"
        }

        test("installs generated type name parser hook exactly") {
            SqlParserUtil.type_name = null

            OracleParserUtil.reset()
            OracleParserUtil.overrideSqlParser()

            SqlParserUtil.type_name.shouldNotBeNull()
        }
    })
