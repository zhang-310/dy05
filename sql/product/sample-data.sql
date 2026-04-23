-- ============================================================
-- product 模块 - 示例数据
-- ============================================================

INSERT INTO dy_product (user_id, product_name, product_category, description, price, cost_price, inventory, sku, tags, status, featured, deleted, create_time, update_time)
VALUES
  (1, '纯棉短袖T恤', '服饰', '100%纯棉面料，透气舒适，多色可选', 79.90, 25.00, 500, 'SKU-TS-001', '纯棉,T恤,夏季', 1, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1, '保湿面膜套装', '美妆', '玻尿酸补水面膜，一盒10片，深层保湿', 59.90, 18.00, 1000, 'SKU-MK-001', '面膜,保湿,护肤', 1, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1, '无线蓝牙耳机', '数码', '降噪蓝牙5.3，续航30小时，IPX5防水', 129.00, 45.00, 300, 'SKU-BT-001', '耳机,蓝牙,降噪', 1, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO dy_product_sales_history (product_id, sale_quantity, sale_amount, sale_time, channel_source, deleted, create_time)
VALUES
  (1, 120, 9588.00, CURRENT_TIMESTAMP - INTERVAL '3 days', '直播', 0, CURRENT_TIMESTAMP),
  (1, 85, 6791.50, CURRENT_TIMESTAMP - INTERVAL '1 day', '短视频', 0, CURRENT_TIMESTAMP),
  (2, 200, 11980.00, CURRENT_TIMESTAMP - INTERVAL '2 days', '直播', 0, CURRENT_TIMESTAMP),
  (3, 50, 6450.00, CURRENT_TIMESTAMP - INTERVAL '1 day', '直播', 0, CURRENT_TIMESTAMP);
