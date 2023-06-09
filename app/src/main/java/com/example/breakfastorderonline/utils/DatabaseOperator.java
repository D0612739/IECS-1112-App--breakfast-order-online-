package com.example.breakfastorderonline.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.breakfastorderonline.utils.models.Cart;
import com.example.breakfastorderonline.utils.models.Menu;
import com.example.breakfastorderonline.utils.models.MenuDishClassification;
import com.example.breakfastorderonline.utils.models.Notification;
import com.example.breakfastorderonline.utils.models.Order;
import com.example.breakfastorderonline.utils.models.OrderDishes;
import com.example.breakfastorderonline.utils.models.OrderState;
import com.example.breakfastorderonline.utils.models.User;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class DatabaseOperator {

    private Context context;
    private DatabaseHelper databaseHelper;
    private SQLiteDatabase db;

    public DatabaseOperator(Context context) {
        this.context = context;
        this.databaseHelper = new DatabaseHelper(context);
        this.db = databaseHelper.getWritableDatabase();
    }

    public void close() {
        if (db.isOpen()) {
            db.close();
        }
        if (databaseHelper != null) {
            databaseHelper.close();
        }
    }

    /**
     * 註冊的時候用來新增使用者資料
     */
    public void addUser(User user) {
        ContentValues values = new ContentValues();
        values.put("account", user.getAccount());
        values.put("password", user.getPassword());
        values.put("email", user.getEmail());
        db.insert("`User`", null, values);
    }

    /**
     * 列出所有使用者資料
     */
    public ArrayList<User> findAllUsers() {
        // define data container and cursor
        ArrayList<User> users = new ArrayList<>();
        // define statement
        String statement = "SELECT `account`, `password`, `email` FROM `User`;";
        // execute query and get a cursor object
        // extract data from cursor
        Cursor cursor = db.rawQuery(statement, null);
        while (cursor.moveToNext()) {  // cursor的index從-1開始，第一次moveToNext之後來到0
            User user = new User(cursor.getString(0), cursor.getString(1), cursor.getString(2));
            users.add(user);
        }
        cursor.close();  // close cursor after used
        return users;
    }

    /**
     * 用account來得到該使用者的資料
     */
    public User findUserByAccount(String userAccount) {
        String statement = "SELECT `account`, `password`, `email` FROM `User` WHERE `account`=?;";
        String[] arguments = new String[]{userAccount};
        Cursor cursor = db.rawQuery(statement, arguments);
        while (cursor.moveToNext()) {
            User user = new User(cursor.getString(0), cursor.getString(1), cursor.getString(2));
            cursor.close();
            return user;
        }
        cursor.close();
        return null;
    }

    /**
     * 用email來得到該使用者的資料
     */
    public User findUserByEmail(String userEmail) {
        String statement = "SELECT `account`, `password`, `email` FROM `User` WHERE `email`=?;";
        String[] arguments = new String[]{userEmail};
        Cursor cursor = db.rawQuery(statement, arguments);
        while (cursor.moveToNext()) {
            User user = new User(cursor.getString(0), cursor.getString(1), cursor.getString(2));
            cursor.close();
            return user;
        }
        cursor.close();
        return null;
    }

    /**
     * 確認email是否已經被使用
     */
    public boolean checkEmailNotExisted(String userEmail) {
        String statement = "SELECT `email` FROM `User` WHERE `email`=?;";
        String[] arguments = new String[]{userEmail};
        Cursor cursor = db.rawQuery(statement, arguments);
        int count = cursor.getCount();
        cursor.close();
        return count == 0;
    }

    /**
     * 更改使用者資料，修改密碼或email
     */
    public void updateUser(User user) {
        ContentValues values = new ContentValues();
        values.put("password", user.getPassword());
        values.put("email", user.getEmail());
        db.update("User", values, "`account`=?", new String[]{user.getAccount()});
    }

    /**
     * 刪除一個使用者帳號
     */
    public void deleteUser(String userAccount) {
        db.delete("User", "`account`=?", new String[]{userAccount});
    }

    /**
     * 列出整個菜單
     */
    public ArrayList<Menu> findAllMenuDishes() {
        ArrayList<Menu> menuDishes = new ArrayList<>();
        String statement = "SELECT `name`, `price` FROM `Menu`;";
        Cursor cursor = db.rawQuery(statement, null);
        while (cursor.moveToNext()) {
            Menu menu = new Menu(cursor.getString(0), cursor.getInt(1));
            menuDishes.add(menu);
        }
        cursor.close();
        return menuDishes;
    }

    /**
     * 新增數個菜單品項，可以用來初始化菜單資料
     */
    public void addManyMenuDishes(ArrayList<Menu> menuDishes) {
        for (Menu menuDish: menuDishes) {
            ContentValues values = new ContentValues();
            values.put("name", menuDish.getName());
            values.put("price", menuDish.getPrice());
            db.insert("Menu", null, values);
        }
    }

    /**
     * 用菜單品項名稱得到該品項的資料
     */
    public Menu findMenuDish(String name) {
        String statement = "SELECT `name`, `price` FROM `Menu` WHERE `name`=?;";
        String[] arguments = new String[]{name};
        Cursor cursor = db.rawQuery(statement, arguments);
        while (cursor.moveToNext()) {
            Menu menu = new Menu(cursor.getString(0), cursor.getInt(1));
            cursor.close();
            return menu;
        }
        cursor.close();
        return null;
    }

    /**
     * 列出該使用者的所有訂單(歷史訂單)
     */
    public ArrayList<Order> findAllOrders(String userAccount) {
        ArrayList<Order> orders = new ArrayList<>();
        String statement = "SELECT `account`, `password`, `email`, " +
            "`id`, `time1`, `time2`, `note`, `state` FROM `Order`, `User` " +
            "WHERE `User`.`account`=`Order`.`user_account` AND `Order`.`user_account`=?;";
        String[] arguments = new String[]{userAccount};
        Cursor cursor = db.rawQuery(statement, arguments);
        while (cursor.moveToNext()) {
            Order order = new Order(
                    cursor.getString(3),
                    new User(cursor.getString(0), cursor.getString(1), cursor.getString(2)),
                    new Date(cursor.getLong(4)),
                    new Date(cursor.getLong(5)),
                    cursor.getString(6),
                    OrderState.valueOf(cursor.getString(7)),
                    getTotalPriceOfOrder(cursor.getString(3))  // total price (Call getTotalPriceOfOrder())
            );
            orders.add(order);
        }
        cursor.close();
        return orders;
    }

    /**
     * 拿到某筆訂單的資料
     */
    public Order findOrder(String orderId) {
        String statement = "SELECT `account`, `password`, `email`, " +
                "`id`, `time1`, `time2`, `note`, `state` FROM `User`, `Order` " +
                "WHERE `id`=? AND `user_account`=`account`;";
        String[] arguments = new String[]{orderId};
        Cursor cursor = db.rawQuery(statement, arguments);
        while (cursor.moveToNext()) {
            Order order = new Order(
                    cursor.getString(3),
                    new User(cursor.getString(0), cursor.getString(1), cursor.getString(2)),
                    new Date(cursor.getLong(4)),
                    new Date(cursor.getLong(5)),
                    cursor.getString(6),
                    OrderState.valueOf(cursor.getString(7)),
                    getTotalPriceOfOrder(cursor.getString(3))
            );
            cursor.close();
            return order;
        }
        cursor.close();
        return null;
    }

    /**
     * 拿到某筆訂單資料的餐點列表
     */
    public ArrayList<OrderDishes> findAllOrderDishesOfOrder(Order order) {
        ArrayList<OrderDishes> orderDishesList = new ArrayList<>();
        String statement = "SELECT " +
                "`OrderDishes`.`order_id`, `OrderDishes`.`dish_name`, `OrderDishes`.`count`, " +
                "`OrderDishes`.`note`, `Menu`.`price` " +
                "FROM `OrderDishes`, `Menu` " +
                "WHERE `order_id`=? AND `dish_name`=`Menu`.`name`;";
        String[] arguments = new String[]{order.getId()};
        Cursor cursor = db.rawQuery(statement, arguments);
        while (cursor.moveToNext()) {
            OrderDishes orderDishes = new OrderDishes(
                    order,
                    new Menu(cursor.getString(1), cursor.getInt(4)),
                    cursor.getInt(2),
                    cursor.getString(3)
            );
            orderDishesList.add(orderDishes);
        }
        cursor.close();
        return orderDishesList;
    }

    /**
     * 新增訂單的餐點明細
     */
    public void addManyOrderDishes(ArrayList<OrderDishes> orderDishesList) {
        for (OrderDishes orderDishes : orderDishesList) {
            ContentValues orderDishValues = new ContentValues();
            orderDishValues.put("order_id", orderDishes.getOrder().getId());
            orderDishValues.put("dish_name", orderDishes.getMenuDish().getName());
            orderDishValues.put("count", orderDishes.getCount());
            orderDishValues.put("note", orderDishes.getNote());
            db.insert("`OrderDishes`", null, orderDishValues);
        }
    }

    /**
     * 新增一個新訂單
     */
    public void addOrder(Order order) {
        if (order == null) {
            return;
        }
        ContentValues orderValues = new ContentValues();
        orderValues.put("id", order.getId());
        orderValues.put("user_account", order.getUser().getAccount());
        orderValues.put("time1", order.getTime1().getTime());
        orderValues.put("time2", order.getTime2().getTime());
        orderValues.put("note", order.getNote());
        orderValues.put("state", order.getState().toString());
        db.insert("`Order`", null, orderValues);
    }

    /**
     * 計算該訂單的總價錢
     */
    public int getTotalPriceOfOrder(String orderId) {
        String statement = "SELECT `price`, `count` FROM `Menu`, `OrderDishes` " +
            "WHERE `order_id`=? AND `Menu`.`name`=`OrderDishes`.`dish_name`;";
        String[] arguments = new String[]{String.valueOf(orderId)};
        Cursor cursor = db.rawQuery(statement, arguments);
        int curCount;
        int curPrice;
        int totalPrice = 0;
        while (cursor.moveToNext()) {
            curPrice = cursor.getInt(0);
            curCount = cursor.getInt(1);
            totalPrice += curPrice * curCount;
        }
        cursor.close();
        return totalPrice;
    }

    public boolean findDishExistedInCart(Cart cart) {
        String statement = "SELECT `Cart`.`user_account`, `Cart`.`dish_name`" +
            "FROM `Cart` WHERE `Cart`.`user_account`=? AND `Cart`.`dish_name`=?;";
        String[] arguments = new String[]{cart.getUser().getAccount(), cart.getMenuDish().getName()};
        Cursor cursor = db.rawQuery(statement, arguments);
        int count = cursor.getCount();
        cursor.close();
        return count > 0;
    }

    /**
     * 新增餐點到購物車
     */
    public void addDishToCart(Cart cart) {
        // check if same dish already existed in cart
        boolean dishExisted = findDishExistedInCart(cart);
        if (dishExisted) {
            return;
        }
        // insert
        ContentValues cartValues = new ContentValues();
        cartValues.put("user_account", cart.getUser().getAccount());
        cartValues.put("dish_name", cart.getMenuDish().getName());
        cartValues.put("count", cart.getCount());
        cartValues.put("note", cart.getNote());
        db.insert("`Cart`", null, cartValues);
    }

    /**
     * 取得當前購物車列表
     */
    public ArrayList<Cart> findAllCartItems(String userAccount) {
        ArrayList<Cart> cartItems = new ArrayList<>();
        String statement = "SELECT " +
                "`User`.`password`, `User`.`email`, `Menu`.`name`, `Menu`.`price`, " +
                "`Cart`.`count`, `Cart`.`note` FROM `User`, `Menu`, `Cart` WHERE " +
                "`User`.`account`=? AND `Cart`.`user_account`=`User`.`account` AND " +
                "`Cart`.`dish_name`=`Menu`.`name`;";
        String[] arguments = new String[]{userAccount};
        Cursor cursor = db.rawQuery(statement, arguments);
        while (cursor.moveToNext()) {
            Cart cartItem = new Cart(
                    new User(userAccount, cursor.getString(0), cursor.getString(1)),
                    new Menu(cursor.getString(2), cursor.getInt(3)),
                    cursor.getInt(4),
                    cursor.getString(5)
            );
            cartItems.add(cartItem);
        }
        cursor.close();
        return cartItems;
    }

    /**
     * 更改購物車內品項的資訊(更改數量或備註)
     */
    public void updateCartItem(Cart cart) {
        ContentValues values = new ContentValues();
        values.put("count", cart.getCount());
        values.put("note", cart.getNote());
        String condition = "`Cart`.`user_account`=? AND `Cart`.`dish_name`=?";
        String[] arguments = new String[]{cart.getUser().getAccount(), cart.getMenuDish().getName()};
        db.update("`Cart`", values, condition, arguments);
    }

    /**
     * 刪除購物車內一項餐點
     */
    public void deleteCartItem(Cart cart) {
        String condition = "`Cart`.`user_account`=? AND `Cart`.`dish_name`=?";
        String[] arguments = new String[]{cart.getUser().getAccount(), cart.getMenuDish().getName()};
        db.delete("`Cart`", condition, arguments);
    }

    /**
     * 清空購物車，在建立訂單資料後會清空購物車
     */
    public void clearCartItems(String userAccount) {
        String condition = "`Cart`.`user_account`=?";
        String[] arguments = new String[]{userAccount};
        db.delete("`Cart`", condition, arguments);
    }

    /**
     * 列出所有通知
     */
    public ArrayList<Notification> findAllNotifications(String userAccount) {
        ArrayList<Notification> notifications = new ArrayList<>();
        String statement = "SELECT " +
            "`Notification`.`time`, `Notification`.`order_id`, `Notification`.`title`, `Notification`.`content`, " +
            "`Order`.`time1`, `Order`.`time2`, `Order`.`note`, `Order`.`state`, `User`.`password`, `User`.`email`, " +
            "`Notification`.`user_read` " +
            "FROM `Notification`, `Order`, `User` " +
            "WHERE `Notification`.`order_id`=`Order`.`id` AND `Order`.`user_account`=`User`.`account` AND `User`.`account`=?;";
        String[] arguments = new String[]{userAccount};
        Cursor cursor = db.rawQuery(statement, arguments);
        while (cursor.moveToNext()) {
            Notification notification = new Notification(
                new Date(cursor.getLong(0)),
                new Order(
                    cursor.getString(1),
                    new User(userAccount, cursor.getString(8), cursor.getString(9)),
                    new Date(cursor.getLong(4)),
                    new Date(cursor.getLong(5)),
                    cursor.getString(6),
                    OrderState.valueOf(cursor.getString(7)),
                    getTotalPriceOfOrder(cursor.getString(1))
                ),
                cursor.getString(2),
                cursor.getString(3),
                (cursor.getInt(10) > 0)
            );
            notifications.add(notification);
        }
        cursor.close();
        return notifications;
    }

    /**
     * 使用者點擊閱讀通知後將該通知設成已讀狀態
     */
    public void updateNotificationRead(Notification notification) {
        ContentValues notificationValues = new ContentValues();
        notificationValues.put("time", notification.getTime().getTime());
        notificationValues.put("order_id", notification.getOrder().getId());
        notificationValues.put("title", notification.getTitle());
        notificationValues.put("content", notification.getContent());
        notificationValues.put("user_read", 1);
        String condition = "`Notification`.`time`=? AND `Notification`.`order_id`=?";
        String[] arguments = new String[]{
                String.valueOf(notification.getTime().getTime()), notification.getOrder().getId()
        };
        db.update("`Notification`", notificationValues, condition, arguments);
    }

    /**
     * 刪除一個通知
     */
    public void deleteNotification(Notification notification) {
        String condition = "`Notification`.`time`=? AND `Notification`.`order_id`=?";
        String[] arguments = new String[]{
                String.valueOf(notification.getTime().getTime()), notification.getOrder().getId()
        };
        db.delete("`Notification`", condition, arguments);
    }

    /**
     * 刪除當前使用者的所有通知
     */
    public void deleteAllNotifications(String userAccount) {
        String condition = "`Notification`.`order_id` IN ( " +
                "SELECT `Order`.`id` FROM `Order` " +
                "WHERE `Order`.`id`=`Notification`.`order_id` AND `Order`.`user_account`=? )";
        String[] arguments = new String[]{userAccount};
        db.delete("`Notification`", condition, arguments);
    }

    public void addNotification(Notification notification) {
        ContentValues notifiValues = new ContentValues();
        notifiValues.put("time", notification.getTime().getTime());
        notifiValues.put("order_id", notification.getOrder().getId());
        notifiValues.put("title", notification.getTitle());
        notifiValues.put("content", notification.getContent());
        notifiValues.put("user_read", 0);
        db.insert("`Notification`", null, notifiValues);
    }

    public MenuDishClassification getMenuDishClassify(Menu menu) {
        Set<String> hamburgerSet = new HashSet<>(
            Arrays.asList(new String[]{"煎蛋漢堡", "起司漢堡", "培根漢堡", "豬肉漢堡", "燻雞漢堡", "牛肉漢堡", "鮪魚漢堡", "香雞漢堡"})
        );
        Set<String> eggrollSet = new HashSet<>(
            Arrays.asList(new String[]{"原味蛋餅", "起司蛋餅", "培根蛋餅", "豬肉蛋餅", "燻雞蛋餅", "玉米蛋餅", "肉鬆蛋餅"})
        );
        Set<String> sandwichSet = new HashSet<>(
            Arrays.asList(new String[]{"煎蛋吐司", "起司吐司", "培根吐司", "豬肉吐司", "燻雞吐司", "牛肉吐司", "鮪魚吐司", "香雞吐司"})
        );
        Set<String> drinkSet = new HashSet<>(
            Arrays.asList(new String[]{"豆漿", "紅茶", "奶茶", "綠茶", "美式咖啡"})
        );

        if (hamburgerSet.contains(menu.getName())) {
            return MenuDishClassification.HAMBURGER;
        } else if (eggrollSet.contains(menu.getName())) {
            return MenuDishClassification.EGGROLL;
        } else if (sandwichSet.contains(menu.getName())) {
            return MenuDishClassification.SANDWICH;
        } else if (drinkSet.contains(menu.getName())) {
            return MenuDishClassification.DRINK;
        }
        return MenuDishClassification.NONE;
    }
}
