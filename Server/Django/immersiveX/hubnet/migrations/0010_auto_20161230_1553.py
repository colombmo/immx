# -*- coding: utf-8 -*-
# Generated by Django 1.10.4 on 2016-12-30 14:53
from __future__ import unicode_literals

from django.db import migrations


class Migration(migrations.Migration):

    dependencies = [
        ('hubnet', '0009_event_profile_image'),
    ]

    operations = [
        migrations.RenameField(
            model_name='event',
            old_name='profile_image',
            new_name='background',
        ),
    ]
