'use client'

import { useState, useEffect } from 'react'
import { createPriceItem, updatePriceItem, deletePriceItem, getPricesFromAdmin, type PriceItem } from '@/lib/admin-api'

const categories = [
  'холодильник',
  'стиральная машина',
  'посудомоечная машина',
  'духовой шкаф',
  'варочная панель',
  'кондиционер',
  'кофемашина',
  'ноутбук',
  'телевизор',
  'микроволновка',
]

export default function AdminPricesPage() {
  const [prices, setPrices] = useState<PriceItem[]>([])
  const [loading, setLoading] = useState(true)
  const [editingItem, setEditingItem] = useState<PriceItem | null>(null)
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false)
  const [filterCategory, setFilterCategory] = useState<string>('')
  const [filterType, setFilterType] = useState<'service' | 'part' | ''>('')
  const [error, setError] = useState<string>('')
  const [success, setSuccess] = useState<string>('')

  // Получаем токен из localStorage или prompt
  const getToken = () => {
    if (typeof window === 'undefined') return ''
    return localStorage.getItem('admin_token') || ''
  }

  const fetchPrices = async () => {
    try {
      setLoading(true)
      const data = await getPricesFromAdmin({
        category: filterCategory || undefined,
        type: filterType || undefined,
      })
      setPrices(data)
      setError('')
    } catch (err: any) {
      setError('Ошибка загрузки прайса: ' + err.message)
      console.error(err)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchPrices()
  }, [filterCategory, filterType])

  const handleCreate = async (formData: Omit<PriceItem, 'id' | 'created_at' | 'updated_at'>) => {
    try {
      const token = getToken()
      if (!token) {
        const newToken = prompt('Введите токен админа:')
        if (!newToken) return
        localStorage.setItem('admin_token', newToken)
      }
      
      await createPriceItem(getToken(), formData)
      setSuccess('Позиция успешно создана')
      setIsCreateModalOpen(false)
      fetchPrices()
      setTimeout(() => setSuccess(''), 3000)
    } catch (err: any) {
      setError('Ошибка создания: ' + err.message)
      setTimeout(() => setError(''), 5000)
    }
  }

  const handleUpdate = async (id: number, formData: Partial<PriceItem>) => {
    try {
      const token = getToken()
      if (!token) {
        const newToken = prompt('Введите токен админа:')
        if (!newToken) return
        localStorage.setItem('admin_token', newToken)
      }
      
      await updatePriceItem(getToken(), id, formData)
      setSuccess('Позиция успешно обновлена')
      setEditingItem(null)
      fetchPrices()
      setTimeout(() => setSuccess(''), 3000)
    } catch (err: any) {
      setError('Ошибка обновления: ' + err.message)
      setTimeout(() => setError(''), 5000)
    }
  }

  const handleDelete = async (id: number) => {
    if (!confirm('Вы уверены, что хотите удалить эту позицию?')) return

    try {
      const token = getToken()
      if (!token) {
        const newToken = prompt('Введите токен админа:')
        if (!newToken) return
        localStorage.setItem('admin_token', newToken)
      }
      
      await deletePriceItem(getToken(), id)
      setSuccess('Позиция успешно удалена')
      fetchPrices()
      setTimeout(() => setSuccess(''), 3000)
    } catch (err: any) {
      setError('Ошибка удаления: ' + err.message)
      setTimeout(() => setError(''), 5000)
    }
  }

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-900 mb-2">Управление прайс-листом</h1>
          <p className="text-gray-600">Добавление, редактирование и удаление позиций прайса</p>
        </div>

        {/* Уведомления */}
        {error && (
          <div className="mb-4 bg-red-50 border border-red-200 text-red-800 px-4 py-3 rounded">
            {error}
          </div>
        )}
        {success && (
          <div className="mb-4 bg-green-50 border border-green-200 text-green-800 px-4 py-3 rounded">
            {success}
          </div>
        )}

        {/* Фильтры и кнопка создания */}
        <div className="bg-white rounded-lg shadow mb-6 p-4">
          <div className="flex flex-wrap gap-4 items-end">
            <div className="flex-1 min-w-[200px]">
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Категория
              </label>
              <select
                value={filterCategory}
                onChange={(e) => setFilterCategory(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                <option value="">Все категории</option>
                {categories.map((cat) => (
                  <option key={cat} value={cat}>
                    {cat}
                  </option>
                ))}
              </select>
            </div>
            <div className="flex-1 min-w-[200px]">
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Тип
              </label>
              <select
                value={filterType}
                onChange={(e) => setFilterType(e.target.value as 'service' | 'part' | '')}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                <option value="">Все типы</option>
                <option value="service">Услуги</option>
                <option value="part">Запчасти</option>
              </select>
            </div>
            <button
              onClick={() => setIsCreateModalOpen(true)}
              className="px-6 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              + Добавить позицию
            </button>
          </div>
        </div>

        {/* Таблица прайса */}
        {loading ? (
          <div className="text-center py-12">
            <div className="inline-block w-8 h-8 border-4 border-gray-200 border-t-blue-600 rounded-full animate-spin"></div>
            <p className="text-gray-600 mt-4">Загрузка...</p>
          </div>
        ) : (
          <div className="bg-white rounded-lg shadow overflow-hidden">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Категория
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Название
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Тип
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Цена
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Ед. изм.
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Действия
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {prices.map((item) => (
                  <tr key={item.id} className="hover:bg-gray-50">
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                      {item.category}
                    </td>
                    <td className="px-6 py-4 text-sm text-gray-900">
                      <div className="font-medium">{item.name}</div>
                      {item.description && (
                        <div className="text-xs text-gray-500 mt-1">{item.description}</div>
                      )}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <span
                        className={`px-2 py-1 text-xs font-semibold rounded-full ${
                          item.type === 'service'
                            ? 'bg-blue-100 text-blue-800'
                            : 'bg-green-100 text-green-800'
                        }`}
                      >
                        {item.type === 'service' ? 'Услуга' : 'Запчасть'}
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                      {item.price.toLocaleString()} ₽
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {item.unit || 'шт'}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                      <button
                        onClick={() => setEditingItem(item)}
                        className="text-blue-600 hover:text-blue-900 mr-4"
                      >
                        Редактировать
                      </button>
                      <button
                        onClick={() => item.id && handleDelete(item.id)}
                        className="text-red-600 hover:text-red-900"
                      >
                        Удалить
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            {prices.length === 0 && (
              <div className="text-center py-12 text-gray-500">
                Позиции не найдены
              </div>
            )}
          </div>
        )}

        {/* Модальное окно создания */}
        {isCreateModalOpen && (
          <PriceModal
            onClose={() => setIsCreateModalOpen(false)}
            onSave={handleCreate}
            categories={categories}
          />
        )}

        {/* Модальное окно редактирования */}
        {editingItem && (
          <PriceModal
            item={editingItem}
            onClose={() => setEditingItem(null)}
            onSave={(data) => editingItem.id && handleUpdate(editingItem.id, data)}
            categories={categories}
          />
        )}
      </div>
    </div>
  )
}

interface PriceModalProps {
  item?: PriceItem
  onClose: () => void
  onSave: (data: Omit<PriceItem, 'id' | 'created_at' | 'updated_at'>) => void
  categories: string[]
}

function PriceModal({ item, onClose, onSave, categories }: PriceModalProps) {
  const [formData, setFormData] = useState({
    category: item?.category || '',
    name: item?.name || '',
    price: item?.price || 0,
    type: item?.type || ('service' as 'service' | 'part'),
    description: item?.description || '',
    unit: item?.unit || 'шт',
  })

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (!formData.category || !formData.name || formData.price <= 0) {
      alert('Заполните все обязательные поля')
      return
    }
    onSave(formData)
  }

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg shadow-xl max-w-2xl w-full mx-4 max-h-[90vh] overflow-y-auto">
        <div className="p-6">
          <h2 className="text-2xl font-bold text-gray-900 mb-4">
            {item ? 'Редактирование позиции' : 'Добавление позиции'}
          </h2>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Категория *
              </label>
              <select
                value={formData.category}
                onChange={(e) => setFormData({ ...formData, category: e.target.value })}
                required
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                <option value="">Выберите категорию</option>
                {categories.map((cat) => (
                  <option key={cat} value={cat}>
                    {cat}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Название *
              </label>
              <input
                type="text"
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                required
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Тип *
                </label>
                <select
                  value={formData.type}
                  onChange={(e) => setFormData({ ...formData, type: e.target.value as 'service' | 'part' })}
                  required
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  <option value="service">Услуга</option>
                  <option value="part">Запчасть</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Цена (₽) *
                </label>
                <input
                  type="number"
                  step="0.01"
                  min="0"
                  value={formData.price}
                  onChange={(e) => setFormData({ ...formData, price: parseFloat(e.target.value) || 0 })}
                  required
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Единица измерения
              </label>
              <input
                type="text"
                value={formData.unit}
                onChange={(e) => setFormData({ ...formData, unit: e.target.value })}
                placeholder="шт, час, услуга и т.д."
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Описание
              </label>
              <textarea
                value={formData.description}
                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                rows={3}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
            <div className="flex justify-end gap-3 pt-4">
              <button
                type="button"
                onClick={onClose}
                className="px-4 py-2 text-gray-700 bg-gray-100 rounded-md hover:bg-gray-200 focus:outline-none focus:ring-2 focus:ring-gray-500"
              >
                Отмена
              </button>
              <button
                type="submit"
                className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                {item ? 'Сохранить' : 'Создать'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  )
}
