package com.example.ordermicroservice.service

import com.example.ordermicroservice.document.Orders
import com.example.ordermicroservice.document.Products
import com.example.ordermicroservice.dto.*
import com.example.ordermicroservice.repository.mongo.OrderRepository
import com.example.ordermicroservice.repository.mongo.SellerRepository
import com.example.ordermicroservice.vo.UserVo
import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockk
import org.springframework.data.redis.core.RedisTemplate

class OrderServiceTest: BehaviorSpec({
    extensions(SpringExtension)

    val products = mutableListOf<Products>()

    var sellerId = ""
    var sellerName = ""

    // @InjectMock
    lateinit var userService: UserService
    lateinit var sellerService: SellerService
    lateinit var orderService: OrderService

    // @Mock
    lateinit var redisService: RedisService
    lateinit var orderRepository: OrderRepository
    lateinit var sellerRepository: SellerRepository

    // initiate mocks
    beforeTest {
        redisService = RedisService(mockk<RedisTemplate<String, Any>>(),
            mockk<RedisTemplate<String, Long>>(), mockk<ObjectMapper>())

        sellerRepository = mockk()
        sellerService = SellerService(sellerRepository, redisService)

        orderRepository = mockk()
        orderService = OrderService(orderRepository, redisService)

    }

    beforeTest {
        MockKAnnotations.init(this, relaxUnitFun = true)

        products.add(
            Products(
                name = "Taylor 314CE",
                quantity = 1,
                price = 3149000
            )
        )
        products.add(
            Products(
                name = "Coca Cola 500ML",
                quantity = 30,
                price = 2300
            )
        )
        products.add(
            Products(
                name = "Mac Book Pro 14",
                quantity = 2,
                price = 2000000
            )
        )

        val registerRequest = RegisterSellerRequest(
            address = "서울특별시 영등포구 도림로131길 17",
            accountNumber = "010-20301-20391",
            phoneNumber = "010-1234-5678",
            businessName = "라라멘"
        )

        every { sellerService.register(registerRequest) } answers { RegisterSellerResponse(
            sellerId = "123",
            businessName = "라라멘",
            created = true
        ) }
        val response = sellerService.register(registerRequest)

        sellerId = response.sellerId
        sellerName = response.businessName

        sellerName shouldBe registerRequest.businessName
        response.created shouldBe true
    }

    given("유저 정보랑 주문 정보가 주어진다.") {
        userService = mockk()
        redisService = mockk()
        orderService = mockk()
        orderRepository = mockk()

        val users = LoginRequest(userId = "Raonpark")
        every { userService.login(users) } answers { LoginResponse(userId = "Raonpark") }
        val userResponse = userService.login(users)

        every { redisService.getUserVo("Raonpark") } answers { UserVo("raon park") }
        val username = redisService.getUserVo(userId = "Raonpark").username

        val order = CreateOrderRequest(
            userId = "Raonpark",
            products = products,
            sellerId = sellerId
        )

        val savedOrders = Orders(
            id = "1234",
            userId = "Raonpark",
            products = products,
            sellerId = sellerId,
            orderNumber = "123",
            orderedTime = "123"
        )

        every { orderService.createOrder(order) } answers { CreateOrderResponse(
            orderNumber = "123",
            address = "123",
            amount = 12341231,
            orderedTime = "123",
            products = products,
            sellerName = sellerName,
            username = "raon park"
        ) }

        When("주문이 들어간다.") {
            val receipt = orderService.createOrder(order)

            then("주문서 정보를 확인한다.") {
                receipt.username shouldBe username
                receipt.sellerName shouldBe sellerName
            }
        }
    }
})