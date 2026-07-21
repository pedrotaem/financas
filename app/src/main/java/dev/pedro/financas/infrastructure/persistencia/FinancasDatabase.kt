package dev.pedro.financas.infrastructure.persistencia

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [LancamentoEntity::class, CapturaBrutaEntity::class, ProcessamentoEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class FinancasDatabase : RoomDatabase() {
    abstract fun lancamentoDao(): LancamentoDao
    abstract fun capturaBrutaDao(): CapturaBrutaDao
    abstract fun processamentoDao(): ProcessamentoDao

    companion object {
        fun criar(context: Context): FinancasDatabase =
            Room.databaseBuilder(context, FinancasDatabase::class.java, "financas.db")
                .build()
    }
}
