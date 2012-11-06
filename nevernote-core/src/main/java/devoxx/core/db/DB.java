package devoxx.core.db;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import devoxx.core.util.F.Function;
import devoxx.core.util.F.Option;
import devoxx.core.util.F.Tuple;
import devoxx.core.util.SimpleLogger;
import java.lang.reflect.Field;
import java.sql.Date;
import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DB {
    
    public static interface Identifiable<ID> {
        ID getId();
    }
    
    private static final ThreadLocal<ConnectionProvider> currentConnectionProvider =
            new ThreadLocal<ConnectionProvider>();
    
    private final ConnectionProvider provider;
    
    private DB(ConnectionProvider provider) {
        this.provider = provider;
        startup();
    }

    private void startup() {
        provider.start();
    }
    
    public final void close() {
        provider.stop();
    }
    
    public final <T> T withConnection(Function<Connection, T> action) {
        currentConnectionProvider.set(provider);
        provider.beforeRequest();
        try {
            SimpleLogger.trace("Connecting to database");
            Connection connection = provider.get();
            try {
                SimpleLogger.trace("Execute user statement");
                return action.apply(connection);
            } catch (Exception e) {
                throw new RuntimeException("Error during SQL execution", e);
            }
        } finally  {
            SimpleLogger.trace("Close connection");
            provider.afterRequest();
            currentConnectionProvider.remove();
        }
    }
    
    public void startConnection() {
        currentConnectionProvider.set(provider);
        provider.beforeRequest();
        Connection connection = provider.get();
    }
    
    public void closeConnection() {
        SimpleLogger.trace("Close connection");
        provider.afterRequest();
        currentConnectionProvider.remove();
    }
    
    /** Public API **/
    
    public static DB DB(ConnectionProvider provider) {
        return new DB(provider);
    }
    
    public static ConnectionProvider provider(Driver driver, String url, String login, String password) {
        return new SimpleProvider(driver, url, login, password);
    }
    
    public static <T> SQLParser<T> parser(final Class<T> clazz, final Extractor<?>... extractors) {
        Option<Function<TypedContainer, T>> opt = Option.none();
        return new SQLParser<T>(clazz, opt, Arrays.asList(extractors));
    }
    
    public static <T> SQLParser<T> parser(final Class<T> clazz, final List<Extractor<?>> extractors) {
        Option<Function<TypedContainer, T>> opt = Option.none();
        return new SQLParser<T>(clazz, opt, extractors);
    }
    
    public static SQLStatement sql(Connection connection, String query) {
        return new SQLStatement(connection, query);
    }
    
    public static SQLStatement sql(String query) {
        return new SQLStatement(DB.currentConnectionProvider.get().get(), query);
    }
    
    public static SQLStatement SQL(Connection connection, String query) {
        return new SQLStatement(connection, query);
    }
    
    public static SQLStatement SQL(String query) {
        return new SQLStatement(DB.currentConnectionProvider.get().get(), query);
    }
   
    public static <T> Extractor<T> get(Class<T> clazz, String name) {
        return new Extractor<T>(clazz, name, false);
    }
    
    public static Pair pair(String key, Object value) {
        return new Pair(key, value);
    }
    
    public static SQLParser<Long> longParser(final String name) {
        return parser(Long.class, get(Long.class, name)).map(new Function<TypedContainer, Long>() {
            @Override
            public Long apply(TypedContainer t) {
                return t.lng(name);
            }
        });
    }
    
    public static SQLParser<Integer> integerParser(final String name) {
        return parser(Integer.class, get(Integer.class, name)).map(new Function<TypedContainer, Integer>() {
            @Override
            public Integer apply(TypedContainer t) {
                return t.intgr(name);
            }
        });
    }
    
    public static SQLParser<Double> doubleParser(final String name) {
        return parser(Double.class, get(Double.class, name)).map(new Function<TypedContainer, Double>() {
            @Override
            public Double apply(TypedContainer t) {
                return t.dbl(name);
            }
        });
    }
    
    public static SQLParser<Date> dateParser(final String name) {
        return parser(Date.class, get(Date.class, name)).map(new Function<TypedContainer, Date>() {
            @Override
            public Date apply(TypedContainer t) {
                return t.date(name);
            }
        });
    }
    
    public static SQLParser<String> StringParser(final String name) {
        return parser(String.class, get(String.class, name)).map(new Function<TypedContainer, String>() {
            @Override
            public String apply(TypedContainer t) {
                return t.str(name);
            }
        });
    }
    
    public static SQLParser<Boolean> booleanParser(final String name) {
        return parser(Boolean.class, get(Boolean.class, name)).map(new Function<TypedContainer, Boolean>() {
            @Override
            public Boolean apply(TypedContainer t) {
                return t.bool(name);
            }
        });
    }
    
    public static interface ConnectionProvider {
        public Connection get();
        public void beforeRequest();
        public void afterRequest();
        public void start();
        public void stop();
    }
    
    private static class SimpleProvider implements ConnectionProvider {
        
        private final String url;
        private final String login;
        private final String password;
        private final Driver driver;
        private static ThreadLocal<Connection> connection = new ThreadLocal<Connection>();
    
        SimpleProvider(Driver driver, String url, String login, String password) {
            this.url = url;
            this.login = login;
            this.password = password;
            this.driver = driver;
        }
        
        @Override
        public final Connection get() {
            return connection.get();
        }
        
        @Override
        public final void beforeRequest() {
            if (connection.get() == null) {
                try {
                    connection.set(DriverManager.getConnection(url, login, password));
                } catch (SQLException e) {
                    throw new RuntimeException("Unable to connect to database", e);
                }
            }
        }
        
        @Override
        public final void afterRequest() {
            if ( connection.get() != null ) {
                try {
                    connection.get().close();
                } catch(Exception e) {
                throw new RuntimeException(e);
                }
            }
            connection.remove();
        }
        
        @Override
        public final void start() {
            SimpleLogger.trace("Setup database connection");
            try {
                DriverManager.registerDriver(driver);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            SimpleLogger.trace("Connection setup done");
        }
        
        @Override
        public final void stop() {
            afterRequest();
        }
    }

    public static class SQLParser<T> {
        private final Class<T> clazz;
        private final List<Extractor<?>> extractors;
        private final Option<Function<TypedContainer, T>> factory;

        SQLParser(Class<T> clazz, Option<Function<TypedContainer, T>> factory, List<Extractor<?>> extractors) {
            this.clazz = clazz;
            this.extractors = extractors;
            this.factory = factory;
        }

        // TODO : need to check row structure (metadata) before parsing
        final Option<T> parseRow(ResultSet rs) throws Exception {
            TypedContainer results = new TypedContainer();
            int j = 1;
            for (Extractor<?> extractor : extractors) {
                results.put(extractor.name, extractor.extract(rs, j));
                j++;
            }
            Object obj = null;
            for (Function<TypedContainer, T> block : factory) {
                obj = block.apply(results);
            }
            return Option.apply((T) obj);
        }

        /**
         * Combine small parsers into bigger ones. Need to redefine map function after
         */
        public final <S, R> SQLParser<R> then(final Class<R> thenClazz, final SQLParser<S> then) {
            List<Extractor<?>> exts = new ArrayList<Extractor<?>>(extractors);
            exts.addAll(then.extractors);
            Option<Function<TypedContainer, R>> opt = Option.none();
            return new SQLParser<R>(thenClazz, opt, exts);
        }

        public final SQLParser<T> map(Function<TypedContainer, T> block) {
            return new SQLParser<T>(this.clazz, Option.apply(block), extractors);
        }
        
        public final SQLParser<T> mapWithFieldsReflection() {
            Function<TypedContainer, T> block = new Function<TypedContainer, T>() {
                @Override
                public T apply(TypedContainer t) {
                    try {
                        return new ReflectMapper<T>(clazz, t).map();
                    } catch (Exception ex) {
                        throw new RuntimeException("Unable to map fields ...", ex);
                    }
                }
            };
            return new SQLParser<T>(this.clazz, Option.apply(block), extractors);
        }
    }
    
    private static class ReflectMapper<T> {
        private final Class<T> clazz;
        private final TypedContainer container;
        ReflectMapper(Class<T> clazz, TypedContainer container) {
            this.clazz = clazz;
            this.container = container;
        }
        
        final T map() throws Exception {
            T instance = clazz.newInstance();
            Set<Field> fields = new HashSet<Field>(Arrays.asList(clazz.getDeclaredFields()));
            fields.addAll(Arrays.asList(clazz.getFields()));
            for (Field field : fields) {
                if (container.values.containsKey(field.getName().toLowerCase())) {
                    field.setAccessible(true);
                    field.set(instance, container.values.get(field.getName().toLowerCase()));
                }
            }
            return instance;
        }
    }

    public static class SQLStatement {
        private final Connection connection;
        private String sql;
        private final Map<String, Pair> pairs = new HashMap<String, Pair>();

        SQLStatement(Connection connection, String sql) {
            this.connection = connection;
            this.sql = sql;
        }

        public final SQLStatement on(Pair... pairs) {
            for (Pair pair : Arrays.asList(pairs)) {
                this.pairs.put(pair.key, pair);
            }
            return this;
        }

        public final <T> List<T> asList(SQLParser<T> parser) {
            return executeQuery(parser);
        }

        public final <T> Option<T> asSingleOpt(SQLParser<T> parser) {
            List<T> res = executeQuery(parser);
            if (!res.isEmpty()) {
                return Option.apply(res.get(0)); // TODO : not efficient at all
            } else {
                return Option.none();
            }
        }
        
        private Tuple<String, List<String>> extractParamNames(String baseSql) {
            String finalSql = baseSql;
            List<String> values = new ArrayList<String>();
            Pattern p = Pattern.compile("\\{[^\\}]+\\}");
            Matcher m = p.matcher(baseSql);
            while (m.find()) {
                values.add(m.group().replace("{", "").replace("}", ""));
            }
            for (String name : values) {
                finalSql = finalSql.replace("{" + name + "}", "?");
            }
            return new Tuple<String, List<String>>(finalSql, values);
        }
        
        private void fillPreparedStatement(PreparedStatement pst, List<String> names) {
            int i = 1;
            for (String name : names) {
                fillOneParam(pst, i, name);
                i++;
            }
        }
        
        private void fillOneParam(PreparedStatement pst, int index, String name) {
            if (pairs.containsKey(name)) {
                Object value = pairs.get(name).value;
                try {
                    if (value instanceof String) {
                        pst.setString(index, (String) value);    
                    } else if (value instanceof Date) {
                        pst.setDate(index, (Date) value);    
                    } else {
                        pst.setObject(index, value);    
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }

        public final boolean execute() {
            try {
                SimpleLogger.trace("Preparing statement");
                Tuple<String, List<String>> vals = extractParamNames(sql);
                PreparedStatement pst = connection.prepareStatement(vals._1);
                fillPreparedStatement(pst, vals._2);
                SimpleLogger.trace("Computed SQL: '" + vals._1 + "'");
                SimpleLogger.trace("Execute query");
                return pst.execute();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            } finally {
                SimpleLogger.trace("Query executed");
            }
        }

        public final int executeUpdate() {
            try {
                SimpleLogger.trace("Preparing statement");
                Tuple<String, List<String>> vals = extractParamNames(sql);
                PreparedStatement pst = connection.prepareStatement(vals._1);
                fillPreparedStatement(pst, vals._2);
                SimpleLogger.trace("Computed SQL: '" + vals._1 + "'");
                SimpleLogger.trace("Execute query");
                return pst.executeUpdate();
            } catch (Exception e) {
                e.printStackTrace();
                return -1;
            } finally {
                SimpleLogger.trace("Query executed");
            }
        }

        private final <T> List<T> executeQuery(SQLParser<T> parser) {
            ResultSet resultSet = null;
            PreparedStatement pst = null;
            try {
                SimpleLogger.trace("Preparing statement");
                Tuple<String, List<String>> vals = extractParamNames(sql);
                pst = connection.prepareStatement(vals._1);
                fillPreparedStatement(pst, vals._2);
                SimpleLogger.trace("Computed SQL: '" + vals._1 + "'");
                SimpleLogger.trace("Execute query");
                resultSet = pst.executeQuery();
                SimpleLogger.trace("Query executed");
                List<T> results = new ArrayList<T>();
                while (resultSet.next()) {
                    Option<T> opt = parser.parseRow(resultSet);
                    for (T obj : opt) {
                        results.add(obj);
                    }
                }
                return results;
            } catch (Exception e) {
                e.printStackTrace();
                return Collections.emptyList();
            } finally {
                SimpleLogger.trace("Close resultSet");
                if ( resultSet != null ) {
                    try {
                        resultSet.close();
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                }
                SimpleLogger.trace("Close statement");
                if ( pst != null ) {
                    try {
                        pst.close();
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public static class Extractor<T> {

        private final Class<?> type;
        private final String name;
        private final boolean opt;

        Extractor(Class<?> type, String name, boolean opt) {
            this.type = type;
            this.name = name;
            this.opt = opt;
        }

        public T extract(ResultSet rs, int index) throws Exception {
            if (type.equals(String.class)) {
                return (T) rs.getString(index);
            }
            if (type.equals(Integer.class)) {
                return (T) Integer.valueOf(rs.getInt(index));
            }
            if (type.equals(Long.class)) {
                return (T) Long.valueOf(rs.getLong(index));
            }
            if (type.equals(Date.class)) {
                return (T) rs.getDate(index);
            }
            if (type.equals(Double.class)) {
                return (T) Double.valueOf(rs.getDouble(index));
            }
            if (type.equals(Boolean.class)) {
                return (T) Boolean.valueOf(rs.getBoolean(index));
            }
            return (T) rs.getObject(index);
        }

        public String name() {
            return name;
        }
        
        public Predicate equal(String equal) {
            return new Predicate("t." + name + " = '" + equal + "'");
        }
        public Predicate equal(int equal) {
            return new Predicate("t." + name + " = " + equal);
        }
        public Predicate equal(double equal) {
            return new Predicate("t." + name + " = " + equal);
        }
        public Predicate equal(long equal) {
            return new Predicate("t." + name + " = " + equal);
        }
        public Predicate equal(Extractor<?> extractor) {
            return new Predicate("t." + name + " = t." + extractor.name);
        }
        public Predicate notEqual(String not) {
            return new Predicate("t." + name + " != '" + not + "'");
        }
        public Predicate notEqual(int not) {
            return new Predicate("t." + name + " != " + not);
        }
        public Predicate notEqual(long not) {
            return new Predicate("t." + name + " != " + not);
        }
        public Predicate notEqual(double not) {
            return new Predicate("t." + name + " != " + not);
        }
        public Predicate notEqual(Extractor<?> not) {
            return new Predicate("t." + name + " != t." + not.name);
        }
        public Predicate greaterThan(String greater) {
            return new Predicate("t." + name + " > '" + greater + "'");
        }
        public Predicate greaterThan(int greater) {
            return new Predicate("t." + name + " > " + greater);
        }
        public Predicate greaterThan(long greater) {
            return new Predicate("t." + name + " > " + greater);
        }
        public Predicate greaterThan(double greater) {
            return new Predicate("t." + name + " > " + greater);
        }
        public Predicate greaterThan(Extractor<?> greater) {
            return new Predicate("t." + name + " > t." + greater.name);
        }
        public Predicate lesserThan(String lesser) {
            return new Predicate("t." + name + " < '" + lesser + "'");
        }
        public Predicate lesserThan(int lesser) {
            return new Predicate("t." + name + " < " + lesser);
        }
        public Predicate lesserThan(double lesser) {
            return new Predicate("t." + name + " < " + lesser);
        }
        public Predicate lesserThan(long lesser) {
            return new Predicate("t." + name + " < " + lesser);
        }
        public Predicate lesserThan(Extractor<?> lesser) {
            return new Predicate("t." + name + " < t." + lesser.name);
        }
        public Predicate greaterEqThan(String greater) {
            return new Predicate("t." + name + " >= '" + greater + "'");
        }
        public Predicate greaterEqThan(int greater) {
            return new Predicate("t." + name + " >= " + greater);
        }
        public Predicate greaterEqThan(double greater) {
            return new Predicate("t." + name + " >= " + greater);
        }
        public Predicate greaterEqThan(long greater) {
            return new Predicate("t." + name + " >= " + greater);
        }
        public Predicate greaterEqThan(Extractor<?> greater) {
            return new Predicate("t." + name + " >= t." + greater.name);
        }
        public Predicate lesserEqThan(String lesser) {
            return new Predicate("t." + name + " <= '" + lesser + "'");
        }
        public Predicate lesserEqThan(int lesser) {
            return new Predicate("t." + name + " <= " + lesser);
        }
        public Predicate lesserEqThan(double lesser) {
            return new Predicate("t." + name + " <= " + lesser);
        }
        public Predicate lesserEqThan(long lesser) {
            return new Predicate("t." + name + " <= " + lesser);
        }
        public Predicate lesserEqThan(Extractor<?> lesser) {
            return new Predicate("t." + name + " <= t." + lesser.name);
        }
        public Predicate like(String like) {
            return new Predicate("t." + name + " like '" + like + "'");
        }
        public Predicate between(int from, int to) {
            return new Predicate("t." + name + " between " + from + " and " + to);
        }
        public Predicate between(double from, double to) {
            return new Predicate("t." + name + " between " + from + " and " + to);
        }
        public Predicate between(long from, long to) {
            return new Predicate("t." + name + " between " + from + " and " + to);
        }
    }
    
    public static abstract class Model implements Identifiable<Long> { }
    
    public static abstract class Table<T extends Identifiable<Long>> {
        public String tableName;
        public Class<T> clazz;
        public String selectAllStatement;
        public String selectByIdStatement;
        public String createStatement;
        public String deleteAllStatement;
        public String deleteStatement;
        public String updateStatement;
        public String countStatement;
        public String ddlDelete;
        private String findWhereStatement;
        
        private List<Extractor<?>> extractors;
        private SQLParser<T> parser;
        
        public <A extends Table<T>> A init(Class<T> clazz, String tableName) {
            this.tableName = tableName;
            this.clazz = clazz;
            this.extractors = all().extractors;
            ExtractorSeq<T> extr = all();
            if (extr.map.isDefined()) {
                this.parser = DB.parser(clazz, extractors).map(extr.map.get());
            } else {
                this.parser = DB.parser(clazz, extractors).mapWithFieldsReflection();
            }
            selectAllStatement = "select " + Joiner.on(", ").join(Iterables.transform(extractors, new com.google.common.base.Function<Extractor<?>, String>() {
                public String apply(Extractor<?> t) {
                    return "t." + t.name();
                }
            })) + " from " + tableName + " t";
            findWhereStatement = "select " + Joiner.on(", ").join(Iterables.transform(extractors, new com.google.common.base.Function<Extractor<?>, String>() {
                public String apply(Extractor<?> t) {
                    return "t." + t.name();
                }
            })) + " from " + tableName + " t where {predicate}";
            selectByIdStatement = "select " + Joiner.on(", ").join(Iterables.transform(extractors, new com.google.common.base.Function<Extractor<?>, String>() {
                public String apply(Extractor<?> t) {
                    return "t." + t.name();
                }
            })) + " from " + tableName + " t where t.id = {id}";
            deleteAllStatement = "delete from " + tableName;
            deleteStatement = "delete from " + tableName + " where id = {id}";
            createStatement = "insert into " + tableName + "(" + Joiner.on(", ").join(Iterables.transform(extractors, new com.google.common.base.Function<Extractor<?>, String>() {
                public String apply(Extractor<?> t) {
                    return t.name();
                }
            })) + ") values ({values})";
            updateStatement = "update " + tableName + " set {values} where id = {id}";
            countStatement = "select count(*) as c from " + tableName;
            ddlDelete = "drop table if exists " + tableName + ";";
            return (A) this;
        }
        
        public abstract ExtractorSeq<T> all();
                
        public static ExtractorSeq seq(Extractor<?>... extractors) {
            return new ExtractorSeq(Arrays.asList(extractors));
        }
        
        public static class ExtractorSeq<T> {
            private List<Extractor<?>> extractors = new ArrayList<Extractor<?>>();
            private Option<Function<TypedContainer, T>> map = Option.none();
            public ExtractorSeq(List<Extractor<?>> extractors) {
                this.extractors.addAll(extractors);
            }
            public ExtractorSeq _(Extractor<?>... extractors) {
                this.extractors.addAll(Arrays.asList(extractors));
                return this;
            }
            public ExtractorSeq map(Function<TypedContainer, T> block) {
                map = Option.apply(block);
                return this;
            }
        }
        
        public void ddlDelete() {
            sql(ddlDelete).executeUpdate();
        }
        
        public List<T> findAll() { 
           return new ArrayList(DB.SQL(selectAllStatement).asList(parser)); 
        }
        
        public List<T> findWhere(String predicate, Pair... params) {
            return new ArrayList(DB.sql(findWhereStatement.replace("{predicate}", predicate)).on(params).asList(parser));
        }
        
        public List<T> find(String sql, Pair... params) {
            return new ArrayList(DB.sql(sql).on(params).asList(parser));
        }

        public Option<T> findById(Long id) {
            return DB.SQL(selectByIdStatement).on(pair("id", id)).asSingleOpt(parser); 
        } 
    
        public T save(T model) {
            if (findById( model.getId() ).isDefined()) {
                return update( model );
            } else {
                return create( model );
            }
        }
        
        public T create(T model) {
            String sql = createStatement.replace("{values}", Joiner.on(", ").join(Iterables.transform(values(model), new com.google.common.base.Function<Pair, String>() {
                public String apply(Pair t) {
                    if (t.value.getClass().equals(String.class)) {
                        return "'" + t.value.toString() + "'";
                    }
                    if (t.value.getClass().equals(Integer.class)) {
                        return t.value.toString();
                    }
                    if (t.value.getClass().equals(Long.class)) {
                        return t.value.toString();
                    }
                    if (t.value.getClass().equals(Double.class)) {
                        return t.value.toString();
                    }
                    if (t.value.getClass().equals(Boolean.class)) {
                        return ((Boolean) t.value) ? "TRUE" : "FALSE";
                    }
                    return "'" + t.value.toString() + "'";
                }
            })));
            sql(sql).executeUpdate();
            return findById(model.getId()).get();
        }
        
        private List<Pair> values(T model) {
            List<Pair> ret = new ArrayList<Pair>();
            Set<Field> fields = new HashSet<Field>(Arrays.asList(clazz.getDeclaredFields()));
            fields.addAll(Arrays.asList(clazz.getFields()));
            for (Extractor e : extractors) {
                for (Field field : fields) {
                    field.setAccessible(true);
                    if (e.name().toLowerCase().equals(field.getName().toLowerCase())) {
                        try {
                            ret.add(pair(e.name(), field.get(model)));
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
            return ret;
        }

        public void delete(Long id) { 
            sql(deleteStatement.replace("{id}", id + "")).executeUpdate();
        }

        public void deleteAll() {
            sql(deleteAllStatement).executeUpdate();
        }

        public T update(T model) {
            String sql = updateStatement.replace("{values}", Joiner.on(", ").join(Iterables.transform(values(model), new com.google.common.base.Function<Pair, String>() {
                public String apply(Pair t) {
                    if (t.value.getClass().equals(String.class)) {
                        return t.key + " = " + "'" + t.value.toString() + "'";
                    }
                    if (t.value.getClass().equals(Integer.class)) {
                        return t.key + " = " + t.value.toString();
                    }
                    if (t.value.getClass().equals(Long.class)) {
                        return t.key + " = " + t.value.toString();
                    }
                    if (t.value.getClass().equals(Double.class)) {
                        return t.key + " = " + t.value.toString();
                    }
                    if (t.value.getClass().equals(Boolean.class)) {
                        return t.key + " = " + (((Boolean) t.value) ? "TRUE" : "FALSE");
                    }
                    return t.key + " = " + "'" + t.value.toString() + "'";
                }
            })));
            sql(sql).on(pair("id", model.getId())).executeUpdate();
            return findById(model.getId()).get();
        }

        public int count(){ 
            return sql(countStatement).asSingleOpt(DB.integerParser("c")).get();
        }

        public boolean exists(Long id) { return findById( id ).isDefined(); }
        public boolean exists(T model) { return findById( model.getId() ).isDefined(); }
        
        public Query<T> filter(Predicate predicate) {
            return new Query<T>(this, predicate); 
        }
        
        public Query<T> orderByAsc(Extractor<?> extractor) {
            Query<T> q = new Query<T>(this, null);
            q.asc = true;
            q.orderCol = extractor.name;
            return q;
        }
        
        public Query<T> orderByDesc(Extractor<?> extractor) {
            Query<T> q = new Query<T>(this, null);
            q.asc = false;
            q.orderCol = extractor.name;
            return q;
        }
        
        public Query<T> drop(int drop) {
            Query<T> q = new Query<T>(this, null);
            q.drop = drop;
            return q;
        }
        
        public Query<T> take(int take) {
            Query<T> q = new Query<T>(this, null);
            q.take = take;
            return q;
        }
        
        public void insertAll(T... objs) {
            for (T obj : Arrays.asList(objs)) {
                save(obj);
            }
        }
    }
    
    public static class Predicate {
        private String sql;
        Predicate(String start) {
            this.sql = start;
        }
        
        public Predicate and(Predicate predicate) {
            sql = sql + " and (" + predicate.toSql() + ")";
            return this;
        }
        
        public Predicate or(Predicate predicate) {
            sql = sql + " or (" + predicate.toSql() + ")";
            return this;
        }
        
        String toSql() {
            return sql;
        }
    }
    
    public static class Query<Q extends Identifiable<Long>> {
        
        private int take = -1;
        private int drop = -1;
        private boolean asc = true;
        private String orderCol = null;
        private final Predicate predicate;
        private Table<Q> table;

        Query(Table<Q> table, Predicate predicate) {
            this.predicate = predicate;
            this.table = table;
        }
                
        public Query orderByAsc(Extractor<?> extractor) {
           this.asc = true;
           this.orderCol = extractor.name;
           return this;
        }
        
        public Query orderByDesc(Extractor<?> extractor) {
           this.asc = false;
           this.orderCol = extractor.name;
           return this; 
        }
        
        public Query drop(int drop) {
            this.drop = drop;
            return this;
        }
        
        public Query take(int take) {
            this.take = take;
            return this;
        }
        
        public List<Q> list() {
            return table.findWhere(toSql());
        }
        
        String toSql() {
            StringBuilder builder = new StringBuilder();
            if (predicate != null) {
                builder.append(predicate.toSql());
            }
            if (orderCol != null) {
                if (asc) 
                    builder.append("order by ").append(orderCol).append(" asc");
                else 
                    builder.append("order by ").append(orderCol).append(" desc");
            }
            if (drop > -1) {
                builder.append(" limit ").append(drop);
            }
            if (drop > -1 && take > -1) {
                builder.append(", ").append(take);
            }
            if (take > -1) {
                builder.append(" limit 0, ").append(take);
            }
            return builder.toString();
        }
    }
    
    public static class TypedContainer {
        
        private final Map<String, Object> values = 
                new HashMap<String, Object>();
        
        TypedContainer() {}
        
        final void put(String key, Object value) {
            values.put(key.toLowerCase(), value);
        }
        
        public final String str(String key) {
            return (String) values.get(key.toLowerCase());
        }
        
        public final Integer intgr(String key) {
            return (Integer) values.get(key.toLowerCase());
        }
        
        public final Long lng(String key) {
            return (Long) values.get(key.toLowerCase());
        }
        
        public final Date date(String key) {
            return (Date) values.get(key.toLowerCase());
        }
        
        public final Boolean bool(String key) {
            return (Boolean) values.get(key.toLowerCase());
        }
        
        public final Double dbl(String key) {
            return (Double) values.get(key);
        }
    }

    public static class Pair{
        final public String key;
        final public Object value;

        Pair(String key, Object value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public final String toString() {
            return "Pair(key: " + key + ", value: " + value + ")";
        }
        
        public final Class<?> type() {
            return value.getClass();
        }
    }   
}
