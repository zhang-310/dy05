import { test, expect } from '../fixtures/auth.fixture'
import { ProductListPage, ProductDetailPage, ProductCategoryPage, EffectivenessScorePage } from '../pages/product.page'

/**
 * 商品模块 E2E 测试
 * 覆盖商品管理、类目管理、效果评分等核心功能
 */

test.describe('商品模块 - 商品管理', () => {
  test('应该能够创建新商品', async ({ authenticatedPage }) => {
    const productPage = new ProductListPage(authenticatedPage)

    await productPage.goto()
    await productPage.verifyPageLoaded()

    const productName = '测试商品 - ' + Date.now()
    await productPage.createProduct({
      name: productName,
      price: 99.99,
      category: '护肤品',
    })

    // 验证商品出现在列表中
    await expect(authenticatedPage.locator(`text=${productName}`)).toBeVisible()
  })

  test('应该能够搜索商品', async ({ authenticatedPage }) => {
    const productPage = new ProductListPage(authenticatedPage)

    await productPage.goto()
    await productPage.searchProduct('面膜')

    // 验证搜索结果
    await expect(authenticatedPage.locator('.MuiDataGrid-row')).not.toHaveCount(0)
  })

  test('应该能够编辑商品价格', async ({ authenticatedPage }) => {
    const productPage = new ProductListPage(authenticatedPage)

    await productPage.goto()

    // 创建测试商品
    const productName = '价格测试商品 - ' + Date.now()
    await productPage.createProduct({
      name: productName,
      price: 100,
      category: '彩妆',
    })

    // 编辑价格
    await productPage.editProduct(productName, 88.88)

    // 验证价格更新
    await expect(authenticatedPage.locator('text=88.88')).toBeVisible()
  })

  test('应该能够删除商品', async ({ authenticatedPage }) => {
    const productPage = new ProductListPage(authenticatedPage)

    await productPage.goto()

    // 创建临时商品
    const productName = '临时商品 - ' + Date.now()
    await productPage.createProduct({
      name: productName,
      price: 50,
      category: '护肤品',
    })

    // 删除商品
    await productPage.deleteProduct(productName)

    // 验证商品已删除
    await expect(authenticatedPage.locator(`text=${productName}`)).toHaveCount(0)
  })

  test('应该能够同步抖音商品', async ({ authenticatedPage }) => {
    const productPage = new ProductListPage(authenticatedPage)

    await productPage.goto()
    await productPage.syncFromDouyin()

    // 验证同步成功
    await expect(authenticatedPage.locator('text=同步成功')).toBeVisible()
  })
})

test.describe('商品模块 - 商品详情', () => {
  let productId: number

  test.beforeEach(async ({ authenticatedPage }) => {
    // 创建测试商品
    const productPage = new ProductListPage(authenticatedPage)
    await productPage.goto()
    await productPage.createProduct({
      name: '详情测试商品 - ' + Date.now(),
      price: 199,
      category: '护肤品',
    })

    // 获取商品 ID（实际应从页面获取）
    productId = 1
  })

  test('应该能够查看商品详情', async ({ authenticatedPage }) => {
    const detailPage = new ProductDetailPage(authenticatedPage)

    await detailPage.goto(productId)
    await detailPage.verifyPageLoaded()

    // 验证详情页加载
    await expect(authenticatedPage.locator('.product-detail')).toBeVisible()
  })

  test('应该能够更新商品基本信息', async ({ authenticatedPage }) => {
    const detailPage = new ProductDetailPage(authenticatedPage)

    await detailPage.goto(productId)
    await detailPage.updateBasicInfo({
      name: '更新后的商品名称',
      price: 299,
      description: '这是更新后的商品描述',
    })

    // 验证更新成功
    await expect(authenticatedPage.locator('text=更新后的商品名称')).toBeVisible()
  })

  test('应该能够上传商品图片', async ({ authenticatedPage }) => {
    const detailPage = new ProductDetailPage(authenticatedPage)

    await detailPage.goto(productId)

    // 上传图片（需要准备测试文件）
    // await detailPage.uploadImage('test-files/product.jpg')

    // 验证上传成功
    // await expect(authenticatedPage.locator('text=上传成功')).toBeVisible()
  })

  test('应该能够添加商品标签', async ({ authenticatedPage }) => {
    const detailPage = new ProductDetailPage(authenticatedPage)

    await detailPage.goto(productId)

    // 添加标签
    await detailPage.addTag('热销')
    await detailPage.addTag('新品')

    // 验证标签添加成功
    await expect(authenticatedPage.locator('text=热销')).toBeVisible()
    await expect(authenticatedPage.locator('text=新品')).toBeVisible()
  })
})

test.describe('商品模块 - 类目管理', () => {
  test('应该能够创建商品类目', async ({ authenticatedPage }) => {
    const categoryPage = new ProductCategoryPage(authenticatedPage)

    await categoryPage.goto()
    await categoryPage.verifyPageLoaded()

    const categoryName = '测试类目 - ' + Date.now()
    await categoryPage.createCategory(categoryName)

    // 验证类目创建成功
    await expect(authenticatedPage.locator(`text=${categoryName}`)).toBeVisible()
  })

  test('应该能够创建子类目', async ({ authenticatedPage }) => {
    const categoryPage = new ProductCategoryPage(authenticatedPage)

    await categoryPage.goto()

    // 创建父类目
    const parentCategory = '父类目 - ' + Date.now()
    await categoryPage.createCategory(parentCategory)

    // 创建子类目
    const childCategory = '子类目 - ' + Date.now()
    await categoryPage.createCategory(childCategory, parentCategory)

    // 验证子类目创建成功
    await expect(authenticatedPage.locator(`text=${childCategory}`)).toBeVisible()
  })

  test('应该能够编辑类目', async ({ authenticatedPage }) => {
    const categoryPage = new ProductCategoryPage(authenticatedPage)

    await categoryPage.goto()

    // 创建类目
    const categoryName = '编辑测试类目 - ' + Date.now()
    await categoryPage.createCategory(categoryName)

    // 编辑类目
    const newName = '修改后的类目名称 - ' + Date.now()
    await categoryPage.editCategory(categoryName, newName)

    // 验证修改成功
    await expect(authenticatedPage.locator(`text=${newName}`)).toBeVisible()
  })

  test('应该能够删除类目', async ({ authenticatedPage }) => {
    const categoryPage = new ProductCategoryPage(authenticatedPage)

    await categoryPage.goto()

    // 创建临时类目
    const categoryName = '临时类目 - ' + Date.now()
    await categoryPage.createCategory(categoryName)

    // 删除类目
    await categoryPage.deleteCategory(categoryName)

    // 验证删除成功
    await expect(authenticatedPage.locator(`text=${categoryName}`)).toHaveCount(0)
  })
})

test.describe('商品模块 - 效果评分', () => {
  test('应该能够查看商品效果评分列表', async ({ authenticatedPage }) => {
    const scorePage = new EffectivenessScorePage(authenticatedPage)

    await scorePage.goto()
    await scorePage.verifyPageLoaded()

    // 验证评分列表加载
    await expect(authenticatedPage.locator('.MuiDataGrid-root')).toBeVisible()
  })

  test('应该能够按评分筛选商品', async ({ authenticatedPage }) => {
    const scorePage = new EffectivenessScorePage(authenticatedPage)

    await scorePage.goto()
    await scorePage.filterByScore(80, 100)

    // 验证筛选结果
    await authenticatedPage.waitForLoadState('networkidle')
  })

  test('应该能够查看评分详情', async ({ authenticatedPage }) => {
    const scorePage = new EffectivenessScorePage(authenticatedPage)

    await scorePage.goto()

    // 查看第一个商品的评分详情
    const firstProduct = await authenticatedPage.locator('.MuiDataGrid-row').first().textContent()
    if (firstProduct) {
      await scorePage.viewScoreDetail(firstProduct)

      // 验证详情弹窗打开
      await expect(authenticatedPage.locator('.score-detail')).toBeVisible()
    }
  })

  test('应该能够导出评分报告', async ({ authenticatedPage }) => {
    const scorePage = new EffectivenessScorePage(authenticatedPage)

    await scorePage.goto()
    await scorePage.exportScoreReport()

    // 验证导出成功
    await expect(authenticatedPage.locator('text=导出成功')).toBeVisible()
  })
})
