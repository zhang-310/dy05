import { Page } from '@playwright/test'
import { waitForPageLoad, expectPageTitle, expectTableLoaded, fillForm, expectToast, selectOption } from '../utils/test-helpers'

/**
 * Page Object Model - 商品模块页面
 */

export class ProductListPage {
  constructor(private page: Page) {}

  async goto() {
    await this.page.goto('/admin/product/list')
    await waitForPageLoad(this.page)
  }

  async verifyPageLoaded() {
    await expectPageTitle(this.page, '商品管理')
    await expectTableLoaded(this.page)
  }

  async createProduct(data: { name: string; price: number; category: string }) {
    await this.page.click('button:has-text("新建商品")')
    await fillForm(this.page, {
      productName: data.name,
      price: String(data.price),
    })
    await selectOption(this.page, '商品类目', data.category)
    await this.page.click('button:has-text("保存")')
    await expectToast(this.page, '保存成功')
  }

  async searchProduct(keyword: string) {
    await this.page.fill('input[placeholder*="搜索"]', keyword)
    await this.page.press('input[placeholder*="搜索"]', 'Enter')
    await waitForPageLoad(this.page)
  }

  async editProduct(productName: string, newPrice: number) {
    const row = this.page.locator(`tr:has-text("${productName}")`)
    await row.locator('button:has-text("编辑")').click()
    await this.page.fill('input[name="price"]', String(newPrice))
    await this.page.click('button:has-text("保存")')
    await expectToast(this.page, '保存成功')
  }

  async deleteProduct(productName: string) {
    const row = this.page.locator(`tr:has-text("${productName}")`)
    await row.locator('button[aria-label="删除"]').click()
    await this.page.click('button:has-text("确认")')
    await expectToast(this.page, '删除成功')
  }

  async syncFromDouyin() {
    await this.page.click('button:has-text("同步抖音商品")')
    await expectToast(this.page, '同步成功')
  }
}

// NOTE: ProductDetailPage and ProductCategoryPage are commented out because
// these routes don't exist in the router. The router only has:
// - /admin/product/list (ProductsPage)
// - /admin/product/effectiveness (EffectivenessScorePage)
// - /admin/product/:productId/scripts (ProductScriptManagePage)
// - /admin/product/:productId/script-versions (ProductScriptVersionPage)

// export class ProductDetailPage {
//   constructor(private page: Page) {}
//
//   async goto(productId: number) {
//     await this.page.goto(`/admin/product/detail?id=${productId}`)
//     await waitForPageLoad(this.page)
//   }
//
//   async verifyPageLoaded() {
//     await expectPageTitle(this.page, '商品详情')
//   }
//
//   async updateBasicInfo(data: { name?: string; price?: number; description?: string }) {
//     await this.page.click('button:has-text("编辑")')
//
//     if (data.name) {
//       await this.page.fill('input[name="productName"]', data.name)
//     }
//     if (data.price) {
//       await this.page.fill('input[name="price"]', String(data.price))
//     }
//     if (data.description) {
//       await this.page.fill('textarea[name="description"]', data.description)
//     }
//
//     await this.page.click('button:has-text("保存")')
//     await expectToast(this.page, '保存成功')
//   }
//
//   async uploadImage(filePath: string) {
//     await this.page.setInputFiles('input[type="file"][accept="image/*"]', filePath)
//     await this.page.waitForSelector('text=上传成功')
//   }
//
//   async addTag(tag: string) {
//     await this.page.fill('input[placeholder*="添加标签"]', tag)
//     await this.page.press('input[placeholder*="添加标签"]', 'Enter')
//   }
// }

// export class ProductCategoryPage {
//   constructor(private page: Page) {}
//
//   async goto() {
//     await this.page.goto('/admin/product/category')
//     await waitForPageLoad(this.page)
//   }
//
//   async verifyPageLoaded() {
//     await expectPageTitle(this.page, '商品类目')
//   }
//
//   async createCategory(name: string, parentCategory?: string) {
//     await this.page.click('button:has-text("新建类目")')
//     await this.page.fill('input[name="categoryName"]', name)
//
//     if (parentCategory) {
//       await selectOption(this.page, '父类目', parentCategory)
//     }
//
//     await this.page.click('button:has-text("保存")')
//     await expectToast(this.page, '保存成功')
//   }
//
//   async editCategory(categoryName: string, newName: string) {
//     const row = this.page.locator(`tr:has-text("${categoryName}")`)
//     await row.locator('button:has-text("编辑")').click()
//     await this.page.fill('input[name="categoryName"]', newName)
//     await this.page.click('button:has-text("保存")')
//     await expectToast(this.page, '保存成功')
//   }
//
//   async deleteCategory(categoryName: string) {
//     const row = this.page.locator(`tr:has-text("${categoryName}")`)
//     await row.locator('button[aria-label="删除"]').click()
//     await this.page.click('button:has-text("确认")')
//     await expectToast(this.page, '删除成功')
//   }
// }

export class EffectivenessScorePage {
  constructor(private page: Page) {}

  async goto() {
    await this.page.goto('/admin/product/effectiveness')
    await waitForPageLoad(this.page)
  }

  async verifyPageLoaded() {
    await expectPageTitle(this.page, '商品效果评分')
    await expectTableLoaded(this.page)
  }

  async filterByScore(minScore: number, maxScore: number) {
    await this.page.fill('input[name="minScore"]', String(minScore))
    await this.page.fill('input[name="maxScore"]', String(maxScore))
    await this.page.click('button:has-text("查询")')
    await waitForPageLoad(this.page)
  }

  async viewScoreDetail(productName: string) {
    const row = this.page.locator(`tr:has-text("${productName}")`)
    await row.locator('button:has-text("详情")').click()
    await this.page.waitForSelector('.score-detail')
  }

  async exportScoreReport() {
    await this.page.click('button:has-text("导出报告")')
    await expectToast(this.page, '导出成功')
  }
}
