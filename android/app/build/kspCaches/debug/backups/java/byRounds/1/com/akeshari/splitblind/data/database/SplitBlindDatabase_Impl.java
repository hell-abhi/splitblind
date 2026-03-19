package com.akeshari.splitblind.data.database;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.akeshari.splitblind.data.database.dao.ExpenseDao;
import com.akeshari.splitblind.data.database.dao.ExpenseDao_Impl;
import com.akeshari.splitblind.data.database.dao.GroupDao;
import com.akeshari.splitblind.data.database.dao.GroupDao_Impl;
import com.akeshari.splitblind.data.database.dao.ProcessedOpDao;
import com.akeshari.splitblind.data.database.dao.ProcessedOpDao_Impl;
import com.akeshari.splitblind.data.database.dao.SettlementDao;
import com.akeshari.splitblind.data.database.dao.SettlementDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class SplitBlindDatabase_Impl extends SplitBlindDatabase {
  private volatile GroupDao _groupDao;

  private volatile ExpenseDao _expenseDao;

  private volatile SettlementDao _settlementDao;

  private volatile ProcessedOpDao _processedOpDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `groups` (`groupId` TEXT NOT NULL, `name` TEXT NOT NULL, `createdBy` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `groupKeyBase64` TEXT NOT NULL, `hlcTimestamp` INTEGER NOT NULL, PRIMARY KEY(`groupId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `members` (`groupId` TEXT NOT NULL, `memberId` TEXT NOT NULL, `displayName` TEXT NOT NULL, `joinedAt` INTEGER NOT NULL, `isDeleted` INTEGER NOT NULL, `hlcTimestamp` INTEGER NOT NULL, PRIMARY KEY(`groupId`, `memberId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `expenses` (`expenseId` TEXT NOT NULL, `groupId` TEXT NOT NULL, `description` TEXT NOT NULL, `amountCents` INTEGER NOT NULL, `currency` TEXT NOT NULL, `paidBy` TEXT NOT NULL, `splitAmong` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `isDeleted` INTEGER NOT NULL, `hlcTimestamp` INTEGER NOT NULL, PRIMARY KEY(`expenseId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `settlements` (`settlementId` TEXT NOT NULL, `groupId` TEXT NOT NULL, `fromMember` TEXT NOT NULL, `toMember` TEXT NOT NULL, `amountCents` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `isDeleted` INTEGER NOT NULL, `hlcTimestamp` INTEGER NOT NULL, PRIMARY KEY(`settlementId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `processed_ops` (`opId` TEXT NOT NULL, `processedAt` INTEGER NOT NULL, PRIMARY KEY(`opId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '97546c973d76c8b92b6f0cb60bdff7cd')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `groups`");
        db.execSQL("DROP TABLE IF EXISTS `members`");
        db.execSQL("DROP TABLE IF EXISTS `expenses`");
        db.execSQL("DROP TABLE IF EXISTS `settlements`");
        db.execSQL("DROP TABLE IF EXISTS `processed_ops`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsGroups = new HashMap<String, TableInfo.Column>(6);
        _columnsGroups.put("groupId", new TableInfo.Column("groupId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGroups.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGroups.put("createdBy", new TableInfo.Column("createdBy", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGroups.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGroups.put("groupKeyBase64", new TableInfo.Column("groupKeyBase64", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGroups.put("hlcTimestamp", new TableInfo.Column("hlcTimestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysGroups = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesGroups = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoGroups = new TableInfo("groups", _columnsGroups, _foreignKeysGroups, _indicesGroups);
        final TableInfo _existingGroups = TableInfo.read(db, "groups");
        if (!_infoGroups.equals(_existingGroups)) {
          return new RoomOpenHelper.ValidationResult(false, "groups(com.akeshari.splitblind.data.database.entity.GroupEntity).\n"
                  + " Expected:\n" + _infoGroups + "\n"
                  + " Found:\n" + _existingGroups);
        }
        final HashMap<String, TableInfo.Column> _columnsMembers = new HashMap<String, TableInfo.Column>(6);
        _columnsMembers.put("groupId", new TableInfo.Column("groupId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMembers.put("memberId", new TableInfo.Column("memberId", "TEXT", true, 2, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMembers.put("displayName", new TableInfo.Column("displayName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMembers.put("joinedAt", new TableInfo.Column("joinedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMembers.put("isDeleted", new TableInfo.Column("isDeleted", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMembers.put("hlcTimestamp", new TableInfo.Column("hlcTimestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysMembers = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesMembers = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoMembers = new TableInfo("members", _columnsMembers, _foreignKeysMembers, _indicesMembers);
        final TableInfo _existingMembers = TableInfo.read(db, "members");
        if (!_infoMembers.equals(_existingMembers)) {
          return new RoomOpenHelper.ValidationResult(false, "members(com.akeshari.splitblind.data.database.entity.MemberEntity).\n"
                  + " Expected:\n" + _infoMembers + "\n"
                  + " Found:\n" + _existingMembers);
        }
        final HashMap<String, TableInfo.Column> _columnsExpenses = new HashMap<String, TableInfo.Column>(10);
        _columnsExpenses.put("expenseId", new TableInfo.Column("expenseId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExpenses.put("groupId", new TableInfo.Column("groupId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExpenses.put("description", new TableInfo.Column("description", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExpenses.put("amountCents", new TableInfo.Column("amountCents", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExpenses.put("currency", new TableInfo.Column("currency", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExpenses.put("paidBy", new TableInfo.Column("paidBy", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExpenses.put("splitAmong", new TableInfo.Column("splitAmong", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExpenses.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExpenses.put("isDeleted", new TableInfo.Column("isDeleted", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExpenses.put("hlcTimestamp", new TableInfo.Column("hlcTimestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysExpenses = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesExpenses = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoExpenses = new TableInfo("expenses", _columnsExpenses, _foreignKeysExpenses, _indicesExpenses);
        final TableInfo _existingExpenses = TableInfo.read(db, "expenses");
        if (!_infoExpenses.equals(_existingExpenses)) {
          return new RoomOpenHelper.ValidationResult(false, "expenses(com.akeshari.splitblind.data.database.entity.ExpenseEntity).\n"
                  + " Expected:\n" + _infoExpenses + "\n"
                  + " Found:\n" + _existingExpenses);
        }
        final HashMap<String, TableInfo.Column> _columnsSettlements = new HashMap<String, TableInfo.Column>(8);
        _columnsSettlements.put("settlementId", new TableInfo.Column("settlementId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSettlements.put("groupId", new TableInfo.Column("groupId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSettlements.put("fromMember", new TableInfo.Column("fromMember", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSettlements.put("toMember", new TableInfo.Column("toMember", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSettlements.put("amountCents", new TableInfo.Column("amountCents", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSettlements.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSettlements.put("isDeleted", new TableInfo.Column("isDeleted", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSettlements.put("hlcTimestamp", new TableInfo.Column("hlcTimestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysSettlements = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesSettlements = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoSettlements = new TableInfo("settlements", _columnsSettlements, _foreignKeysSettlements, _indicesSettlements);
        final TableInfo _existingSettlements = TableInfo.read(db, "settlements");
        if (!_infoSettlements.equals(_existingSettlements)) {
          return new RoomOpenHelper.ValidationResult(false, "settlements(com.akeshari.splitblind.data.database.entity.SettlementEntity).\n"
                  + " Expected:\n" + _infoSettlements + "\n"
                  + " Found:\n" + _existingSettlements);
        }
        final HashMap<String, TableInfo.Column> _columnsProcessedOps = new HashMap<String, TableInfo.Column>(2);
        _columnsProcessedOps.put("opId", new TableInfo.Column("opId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProcessedOps.put("processedAt", new TableInfo.Column("processedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysProcessedOps = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesProcessedOps = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoProcessedOps = new TableInfo("processed_ops", _columnsProcessedOps, _foreignKeysProcessedOps, _indicesProcessedOps);
        final TableInfo _existingProcessedOps = TableInfo.read(db, "processed_ops");
        if (!_infoProcessedOps.equals(_existingProcessedOps)) {
          return new RoomOpenHelper.ValidationResult(false, "processed_ops(com.akeshari.splitblind.data.database.entity.ProcessedOpEntity).\n"
                  + " Expected:\n" + _infoProcessedOps + "\n"
                  + " Found:\n" + _existingProcessedOps);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "97546c973d76c8b92b6f0cb60bdff7cd", "3e2408715281a63e2b600592b27ee3ff");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "groups","members","expenses","settlements","processed_ops");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `groups`");
      _db.execSQL("DELETE FROM `members`");
      _db.execSQL("DELETE FROM `expenses`");
      _db.execSQL("DELETE FROM `settlements`");
      _db.execSQL("DELETE FROM `processed_ops`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(GroupDao.class, GroupDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(ExpenseDao.class, ExpenseDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(SettlementDao.class, SettlementDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(ProcessedOpDao.class, ProcessedOpDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public GroupDao groupDao() {
    if (_groupDao != null) {
      return _groupDao;
    } else {
      synchronized(this) {
        if(_groupDao == null) {
          _groupDao = new GroupDao_Impl(this);
        }
        return _groupDao;
      }
    }
  }

  @Override
  public ExpenseDao expenseDao() {
    if (_expenseDao != null) {
      return _expenseDao;
    } else {
      synchronized(this) {
        if(_expenseDao == null) {
          _expenseDao = new ExpenseDao_Impl(this);
        }
        return _expenseDao;
      }
    }
  }

  @Override
  public SettlementDao settlementDao() {
    if (_settlementDao != null) {
      return _settlementDao;
    } else {
      synchronized(this) {
        if(_settlementDao == null) {
          _settlementDao = new SettlementDao_Impl(this);
        }
        return _settlementDao;
      }
    }
  }

  @Override
  public ProcessedOpDao processedOpDao() {
    if (_processedOpDao != null) {
      return _processedOpDao;
    } else {
      synchronized(this) {
        if(_processedOpDao == null) {
          _processedOpDao = new ProcessedOpDao_Impl(this);
        }
        return _processedOpDao;
      }
    }
  }
}
