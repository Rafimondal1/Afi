package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun ApkExportGuideDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(TvBullGreenBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        tint = TvBullGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "APK এক্সপোর্ট নির্দেশিকা",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp
                    )
                    Text(
                        text = "Where to find the APK Export Option",
                        color = TvTextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Highlight box representing the AI Studio Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(TvDarkBackground)
                        .border(1.dp, TvBrandBlue.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "AI Studio স্ক্রিনের অবস্থান (Location in Browser UI):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TvBrandBlueLight
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // Mock Browser Bar illustration
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(TvDarkSurface, RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TradeView Pro · AI Studio",
                            fontSize = 11.sp,
                            color = TvTextSecondary
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(TvBrandBlue, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Download, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Download APK", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                            Icon(Icons.Default.Settings, contentDescription = null, tint = TvGold, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                // Step-by-Step Instructions
                StepItem(
                    stepNum = "1",
                    titleBn = "ব্রাউজারের উপরের ডানদিকের হেডার দেখুন",
                    titleEn = "Look at the top-right header in AI Studio",
                    desc = "AI Studio উইন্ডোর একদম উপরের ডান কোণায় (Settings ⚙️ বা শেয়ার বাটনের পাশে) দেখুন।"
                )

                StepItem(
                    stepNum = "2",
                    titleBn = "Settings (গিয়ার আইকন ⚙️) বা Export মেনুতে ক্লিক করুন",
                    titleEn = "Click Settings or Export Menu",
                    desc = "সেখানে ক্লিক করলেই ড্রপডাউন অপশনগুলো দৃশ্যমান হবে।"
                )

                StepItem(
                    stepNum = "3",
                    titleBn = "'Download APK' নির্বাচন করুন",
                    titleEn = "Select 'Download APK'",
                    desc = "এটি কোনো .zip ফাইল নয়, বরং সরাসরি আপনার ফোনে ইনস্টলযোগ্য .apk ফাইল হিসেবে ডাউনলোড হবে।"
                )

                StepItem(
                    stepNum = "4",
                    titleBn = "ফোনে এক ক্লিকে ইনস্টল করুন",
                    titleEn = "Install directly on Android",
                    desc = "ডাউনলোড হওয়া APK ফাইলটিতে ক্লিক করে Install নির্বাচন করলেই অ্যাপটি চালু হয়ে যাবে।"
                )

                // Status info
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TvBullGreenBg, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = TvBullGreen, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "বিল্ড সফলভাবে সম্পন্ন এবং APK প্রস্তুত আছে (আকার < ২০ MB)।",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TvBullGreen
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = TvBrandBlue),
                modifier = Modifier.testTag("dismiss_export_guide_btn")
            ) {
                Text("বুঝেছি (Got It)", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = TvDarkSurface
    )
}

@Composable
private fun StepItem(
    stepNum: String,
    titleBn: String,
    titleEn: String,
    desc: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(TvBrandBlue),
            contentAlignment = Alignment.Center
        ) {
            Text(text = stepNum, color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(text = titleBn, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(text = titleEn, fontSize = 10.sp, color = TvBrandBlueLight)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = desc, fontSize = 11.sp, color = TvTextSecondary, lineHeight = 16.sp)
        }
    }
}
