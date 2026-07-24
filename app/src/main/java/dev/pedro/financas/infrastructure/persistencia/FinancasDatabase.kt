package dev.pedro.financas.infrastructure.persistencia

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        LancamentoEntity::class,
        CapturaBrutaEntity::class,
        ProcessamentoEntity::class,
        OrcamentoEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class FinancasDatabase : RoomDatabase() {
    abstract fun lancamentoDao(): LancamentoDao
    abstract fun capturaBrutaDao(): CapturaBrutaDao
    abstract fun processamentoDao(): ProcessamentoDao
    abstract fun orcamentoDao(): OrcamentoDao

    companion object {
        /** Spec 006: tabela de orçamentos, preservando dados existentes. */
        private val MIGRACAO_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `orcamentos` (" +
                        "`categoria` TEXT NOT NULL, " +
                        "`valorCentavos` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`categoria`))"
                )
            }
        }

        fun criar(context: Context): FinancasDatabase =
            Room.databaseBuilder(context, FinancasDatabase::class.java, "financas.db")
                .addMigrations(MIGRACAO_1_2)
                .build()
    }
}
